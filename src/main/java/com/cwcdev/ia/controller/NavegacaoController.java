package com.cwcdev.ia.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.service.NavegacaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class NavegacaoController {

    @Autowired
    private NavegacaoService navegacaoService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private List<Endereco> historicoGeral = new ArrayList<>();
    private Rota rotaAtual;
    private boolean navegacaoAtiva = false;
    private int instrucaoAtualIndex = 0;
    private PosicaoAtual posicaoUsuario;

    @PostMapping("/buscar-cep")
    public String buscarPorCep(@RequestParam String cep, Model model) {
        Endereco endereco = navegacaoService.buscarEnderecoPorCep(cep);
        
        if (!endereco.isErro()) {
            adicionarAoHistorico(endereco);
        }
        
        model.addAttribute("endereco", endereco);
        model.addAttribute("cepPesquisado", cep);
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("rota", rotaAtual);
        model.addAttribute("navegacaoAtiva", navegacaoAtiva);
        model.addAttribute("instrucaoAtualIndex", instrucaoAtualIndex);
        
        return "index";
    }

    @PostMapping("/calcular-rota")
    public String calcularRota(
            @RequestParam String origemCep,
            @RequestParam String destinoCep,
            Model model) {
        
        try {
            System.out.println("=== CALCULANDO ROTA ===");
            System.out.println("Origem: " + origemCep);
            System.out.println("Destino: " + destinoCep);
            
            Endereco origem;
            Endereco destino;
            
            // Verificar se são coordenadas
            if (origemCep.startsWith("COORD:")) {
                origem = criarEnderecoDeCoordenadas(origemCep);
            } else {
                origem = navegacaoService.buscarEnderecoPorCep(origemCep);
            }
            
            if (destinoCep.startsWith("COORD:")) {
                destino = criarEnderecoDeCoordenadas(destinoCep);
            } else {
                destino = navegacaoService.buscarEnderecoPorCep(destinoCep);
            }
            
            if (origem.isErro()) {
                model.addAttribute("erroRota", "Origem não encontrada: " + origemCep);
                return "index";
            }
            
            if (destino.isErro()) {
                model.addAttribute("erroRota", "Destino não encontrado: " + destinoCep);
                return "index";
            }
            
            System.out.println("Calculando rota entre coordenadas...");
            System.out.println("Origem coord: " + origem.getLatitude() + ", " + origem.getLongitude());
            System.out.println("Destino coord: " + destino.getLatitude() + ", " + destino.getLongitude());
            
            rotaAtual = navegacaoService.calcularRota(origem, destino);
            
            if (rotaAtual != null) {
                System.out.println("✓ Rota calculada com sucesso!");
                System.out.println("  - Distância: " + (rotaAtual.getDistancia() / 1000) + " km");
                System.out.println("  - Duração: " + (rotaAtual.getDuracao() / 60) + " min");
                System.out.println("  - Instruções: " + rotaAtual.getInstrucoes().size());
                
                adicionarAoHistorico(origem);
                adicionarAoHistorico(destino);
                
                model.addAttribute("sucessoRota", "Rota calculada! Distância: " + 
                    String.format("%.2f", rotaAtual.getDistancia() / 1000) + " km");
                
                // Resetar navegação
                navegacaoAtiva = false;
                instrucaoAtualIndex = 0;
            } else {
                model.addAttribute("erroRota", "Não foi possível calcular a rota");
            }
            
            model.addAttribute("origem", origem);
            model.addAttribute("destino", destino);
            model.addAttribute("rota", rotaAtual);
            model.addAttribute("historico", historicoGeral);
            model.addAttribute("navegacaoAtiva", navegacaoAtiva);
            model.addAttribute("instrucaoAtualIndex", instrucaoAtualIndex);
            
        } catch (Exception e) {
            System.err.println("✗ Erro ao calcular rota: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("erroRota", "Erro ao calcular rota: " + e.getMessage());
        }
        
        return "index";
    }

    // Método auxiliar para criar Endereco a partir de coordenadas
    private Endereco criarEnderecoDeCoordenadas(String coordString) {
        try {
            // Formato: COORD:lat,lng
            String coordPart = coordString.replace("COORD:", "");
            String[] parts = coordPart.split(",");
            
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0]);
                double lng = Double.parseDouble(parts[1]);
                
                Endereco endereco = new Endereco();
                endereco.setCep("COORDENADA");
                endereco.setLogradouro("Localização: " + String.format("%.6f", lat) + ", " + String.format("%.6f", lng));
                endereco.setLocalidade("Coordenada GPS");
                endereco.setUf("GPS");
                endereco.setLatitude(lat);
                endereco.setLongitude(lng);
                endereco.setErro(false);
                
                return endereco;
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear coordenadas: " + e.getMessage());
        }
        
        Endereco erro = new Endereco();
        erro.setErro(true);
        return erro;
    }

    // WebSocket endpoints para navegação em tempo real

    @MessageMapping("/navegacao.iniciar")
    @SendTo("/topic/navegacao")
    public String iniciarNavegacao() {
        if (rotaAtual != null) {
            navegacaoAtiva = true;
            instrucaoAtualIndex = 0;
            System.out.println("▶ Navegação iniciada");
            
            if (!rotaAtual.getInstrucoes().isEmpty()) {
                String primeiraInstrucao = rotaAtual.getInstrucoes().get(0).getInstrucao();
                messagingTemplate.convertAndSend("/topic/instrucoes", 
                    criarRespostaInstrucao(primeiraInstrucao, 0));
                System.out.println("  Primeira instrução: " + primeiraInstrucao);
            }
            
            return "NAVEGACAO_INICIADA";
        }
        return "ERRO: Nenhuma rota definida";
    }

    @MessageMapping("/navegacao.parar")
    @SendTo("/topic/navegacao")
    public String pararNavegacao() {
        navegacaoAtiva = false;
        instrucaoAtualIndex = 0;
        System.out.println("■ Navegação parada");
        return "NAVEGACAO_PARADA";
    }

    @MessageMapping("/navegacao.proxima")
    @SendTo("/topic/instrucoes")
    public String proximaInstrucao() {
        if (rotaAtual != null && navegacaoAtiva) {
            if (instrucaoAtualIndex < rotaAtual.getInstrucoes().size() - 1) {
                instrucaoAtualIndex++;
                String instrucao = rotaAtual.getInstrucoes().get(instrucaoAtualIndex).getInstrucao();
                System.out.println("→ Próxima instrução [" + instrucaoAtualIndex + "]: " + instrucao);
                return criarRespostaInstrucao(instrucao, instrucaoAtualIndex);
            } else {
                navegacaoAtiva = false;
                System.out.println("🏁 Chegada ao destino!");
                return criarRespostaInstrucao("🎉 Você chegou ao destino!", instrucaoAtualIndex);
            }
        }
        return "ERRO: Navegação não iniciada";
    }

    @MessageMapping("/gps.posicao")
    @SendTo("/topic/posicao")
    public String atualizarPosicao(String posicaoJson) {
        try {
            JsonNode posicao = objectMapper.readTree(posicaoJson);
            double lat = posicao.get("latitude").asDouble();
            double lng = posicao.get("longitude").asDouble();
            double accuracy = posicao.get("accuracy").asDouble();
            double speed = posicao.has("speed") ? posicao.get("speed").asDouble() : 0;
            
            posicaoUsuario = new PosicaoAtual(lat, lng, accuracy, speed);
            
            System.out.println("📍 Posição: " + String.format("%.6f", lat) + ", " + 
                             String.format("%.6f", lng) + " (±" + Math.round(accuracy) + "m) " +
                             "Vel: " + String.format("%.1f", speed * 3.6) + " km/h");
            
            // Verificar proximidade com próxima manobra
            if (rotaAtual != null && navegacaoAtiva && 
                instrucaoAtualIndex < rotaAtual.getInstrucoes().size()) {
                
                var instrucaoAtual = rotaAtual.getInstrucoes().get(instrucaoAtualIndex);
                
                if (instrucaoAtual.getLatitude() != null && instrucaoAtual.getLongitude() != null) {
                    double distancia = calcularDistancia(lat, lng, 
                        instrucaoAtual.getLatitude(), instrucaoAtual.getLongitude());
                    
                    // Alerta de proximidade (200m)
                    if (distancia < 200 && distancia > 100 && !instrucaoAtual.isAlertaEmitido()) {
                        messagingTemplate.convertAndSend("/topic/alerta", 
                            criarAlertaProximidade(distancia, instrucaoAtual.getInstrucao()));
                        instrucaoAtual.setAlertaEmitido(true);
                    }
                    
                    // Avançar instrução automaticamente (50m)
                    if (distancia < 50 && instrucaoAtualIndex < rotaAtual.getInstrucoes().size() - 1) {
                        proximaInstrucao();
                    }
                }
            }
            
            // Verificar proximidade com destino
            if (rotaAtual != null && navegacaoAtiva) {
                Endereco destino = rotaAtual.getDestino();
                if (destino.getLatitude() != null && destino.getLongitude() != null) {
                    double distanciaDestino = calcularDistancia(lat, lng, 
                        destino.getLatitude(), destino.getLongitude());
                    
                    System.out.println("  Distância até destino: " + Math.round(distanciaDestino) + "m");
                    
                    if (distanciaDestino < 50) {
                        messagingTemplate.convertAndSend("/topic/chegada", 
                            "CHEGADA: Você está a " + Math.round(distanciaDestino) + "m do destino!");
                        navegacaoAtiva = false;
                    }
                }
            }
            
            return criarRespostaPosicao(lat, lng, accuracy, speed);
            
        } catch (Exception e) {
            System.err.println("✗ Erro ao processar posição GPS: " + e.getMessage());
            return "ERRO: " + e.getMessage();
        }
    }

    @GetMapping("/limpar")
    public String limpar(Model model) {
        historicoGeral.clear();
        rotaAtual = null;
        navegacaoAtiva = false;
        instrucaoAtualIndex = 0;
        posicaoUsuario = null;
        
        System.out.println("🗑 Histórico e navegação limpos");
        
        model.addAttribute("endereco", new Endereco());
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("rota", null);
        model.addAttribute("navegacaoAtiva", false);
        model.addAttribute("instrucaoAtualIndex", 0);
        
        return "index";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("endereco", new Endereco());
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("rota", rotaAtual);
        model.addAttribute("navegacaoAtiva", navegacaoAtiva);
        model.addAttribute("instrucaoAtualIndex", instrucaoAtualIndex);
        return "index";
    }

    // Métodos auxiliares
    
    private void adicionarAoHistorico(Endereco endereco) {
        // Evitar duplicatas
        boolean existe = historicoGeral.stream()
            .anyMatch(e -> e.getCep() != null && e.getCep().equals(endereco.getCep()));
        
        if (!existe) {
            historicoGeral.add(0, endereco);
            if (historicoGeral.size() > 10) {
                historicoGeral = historicoGeral.subList(0, 10);
            }
        }
    }

    private String criarRespostaInstrucao(String instrucao, int index) {
        try {
            return objectMapper.writeValueAsString(new RespostaInstrucao(instrucao, index));
        } catch (Exception e) {
            return "{\"instrucao\": \"" + instrucao + "\", \"index\": " + index + "}";
        }
    }

    private String criarRespostaPosicao(double lat, double lng, double accuracy, double speed) {
        try {
            return objectMapper.writeValueAsString(new RespostaPosicao(lat, lng, accuracy, speed));
        } catch (Exception e) {
            return "{\"latitude\": " + lat + ", \"longitude\": " + lng + 
                   ", \"accuracy\": " + accuracy + ", \"speed\": " + speed + "}";
        }
    }

    private String criarAlertaProximidade(double distancia, String instrucao) {
        try {
            AlertaProximidade alerta = new AlertaProximidade(
                Math.round(distancia), 
                instrucao,
                "Em " + Math.round(distancia) + " metros, " + instrucao
            );
            return objectMapper.writeValueAsString(alerta);
        } catch (Exception e) {
            return "{\"distancia\": " + distancia + ", \"instrucao\": \"" + instrucao + "\"}";
        }
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // Classes internas para respostas JSON
    
    private static class RespostaInstrucao {
        public String instrucao;
        public int index;
        public long timestamp;
        
        public RespostaInstrucao(String instrucao, int index) {
            this.instrucao = instrucao;
            this.index = index;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class RespostaPosicao {
        public double latitude;
        public double longitude;
        public double accuracy;
        public double speed;
        public long timestamp;
        
        public RespostaPosicao(double latitude, double longitude, double accuracy, double speed) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.speed = speed;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class AlertaProximidade {
        public long distancia;
        public String instrucao;
        public String mensagem;
        public String tipo;
        public long timestamp;
        
        public AlertaProximidade(long distancia, String instrucao, String mensagem) {
            this.distancia = distancia;
            this.instrucao = instrucao;
            this.mensagem = mensagem;
            this.tipo = "PROXIMIDADE";
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class PosicaoAtual {
        public double latitude;
        public double longitude;
        public double accuracy;
        public double speed;
        public long timestamp;
        
        public PosicaoAtual(double latitude, double longitude, double accuracy, double speed) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.speed = speed;
            this.timestamp = System.currentTimeMillis();
        }
    }
}