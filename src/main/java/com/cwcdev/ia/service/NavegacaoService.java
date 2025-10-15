package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.model.InstrucaoNavegacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class NavegacaoService {

    private static final String VIA_CEP_URL = "https://viacep.com.br/ws/";
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NavegacaoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public Endereco buscarEnderecoPorCep(String cep) {
        // Se for coordenada, retornar diretamente
        if (cep.startsWith("COORD:")) {
            return criarEnderecoDeCoordenadas(cep);
        }
        
        cep = cep.replaceAll("[^0-9]", "");
        
        if (cep.length() != 8) {
            return criarEnderecoComErro("CEP inválido");
        }
        
        String url = VIA_CEP_URL + cep + "/json/";
        
        try {
            ResponseEntity<Endereco> response = restTemplate.getForEntity(url, Endereco.class);
            Endereco endereco = response.getBody();
            
            if (endereco != null && endereco.getCep() == null) {
                endereco.setErro(true);
            } else if (endereco != null && !endereco.isErro()) {
                buscarCoordenadas(endereco);
            }
            
            return endereco;
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar CEP: " + e.getMessage());
            return criarEnderecoComErro("Erro na busca do CEP");
        }
    }

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
                endereco.setLogradouro("Localização GPS");
                endereco.setLocalidade("Coordenada");
                endereco.setUf("GPS");
                endereco.setLatitude(lat);
                endereco.setLongitude(lng);
                endereco.setErro(false);
                
                return endereco;
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear coordenadas: " + e.getMessage());
        }
        
        return criarEnderecoComErro("Coordenadas inválidas");
    }

    private boolean buscarCoordenadas(Endereco endereco) {
        try {
            String enderecoCompleto = construirEnderecoCompleto(endereco);
            
            String url = String.format("%s?format=json&q=%s&limit=1", 
                NOMINATIM_URL, 
                java.net.URLEncoder.encode(enderecoCompleto, "UTF-8"));
            
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("User-Agent", "GPS-Navegacao-App/1.0");
                return execution.execute(request, body);
            });
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.isArray() && root.size() > 0) {
                JsonNode firstResult = root.get(0);
                endereco.setLatitude(firstResult.get("lat").asDouble());
                endereco.setLongitude(firstResult.get("lon").asDouble());
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar coordenadas: " + e.getMessage());
        }
        return false;
    }

    private String construirEnderecoCompleto(Endereco endereco) {
        StringBuilder enderecoCompleto = new StringBuilder();
        
        if (endereco.getLogradouro() != null && !endereco.getLogradouro().isEmpty()) {
            enderecoCompleto.append(endereco.getLogradouro());
        }
        
        if (endereco.getBairro() != null && !endereco.getBairro().isEmpty()) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append(", ");
            enderecoCompleto.append(endereco.getBairro());
        }
        
        if (endereco.getLocalidade() != null && !endereco.getLocalidade().isEmpty()) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append(", ");
            enderecoCompleto.append(endereco.getLocalidade());
        }
        
        if (endereco.getUf() != null && !endereco.getUf().isEmpty()) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append(", ");
            enderecoCompleto.append(endereco.getUf());
        }
        
        return enderecoCompleto.toString();
    }

    public Rota calcularRota(Endereco origem, Endereco destino) {
        try {
            // Garantir que temos coordenadas
            if (origem.getLatitude() == null || origem.getLongitude() == null) {
                if (!buscarCoordenadas(origem)) {
                    throw new RuntimeException("Não foi possível obter coordenadas da origem");
                }
            }
            
            if (destino.getLatitude() == null || destino.getLongitude() == null) {
                if (!buscarCoordenadas(destino)) {
                    throw new RuntimeException("Não foi possível obter coordenadas do destino");
                }
            }
            
            System.out.println("Coordenadas origem: " + origem.getLatitude() + ", " + origem.getLongitude());
            System.out.println("Coordenadas destino: " + destino.getLatitude() + ", " + destino.getLongitude());
            
            String coordenadas = String.format("%s,%s;%s,%s", 
                origem.getLongitude(), origem.getLatitude(),
                destino.getLongitude(), destino.getLatitude());
            
            // Solicitar geometria completa e instruções detalhadas
            String url = OSRM_URL + coordenadas + "?overview=full&steps=true&geometries=polyline&annotations=true";
            
            System.out.println("URL OSRM: " + url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            
            System.out.println("Resposta OSRM: " + root.get("code").asText());
            
            if (root.get("code").asText().equals("Ok")) {
                JsonNode routes = root.get("routes");
                JsonNode route = routes.get(0);
                
                Rota rota = new Rota();
                rota.setOrigem(origem);
                rota.setDestino(destino);
                rota.setDistancia(route.get("distance").asDouble());
                rota.setDuracao(route.get("duration").asDouble());
                rota.setDadosRotaCompleta(root);
                
                // Processar instruções de navegação
                List<InstrucaoNavegacao> instrucoes = processarInstrucoes(route);
                rota.setInstrucoes(instrucoes);
                
                System.out.println("Rota criada com " + instrucoes.size() + " instruções");
                return rota;
            } else {
                throw new RuntimeException("Erro OSRM: " + root.get("message").asText());
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao calcular rota: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao calcular rota: " + e.getMessage());
        }
    }
    
    private List<InstrucaoNavegacao> processarInstrucoes(JsonNode route) {
        List<InstrucaoNavegacao> instrucoes = new ArrayList<>();
        
        try {
            JsonNode legs = route.get("legs");
            JsonNode leg = legs.get(0);
            JsonNode steps = leg.get("steps");
            
            double distanciaAcumulada = 0;
            
            for (int i = 0; i < steps.size(); i++) {
                JsonNode step = steps.get(i);
                
                // Pular instruções muito curtas (menos de 10m)
                double distancia = step.get("distance").asDouble();
                if (distancia < 10 && i > 0 && i < steps.size() - 1) {
                    continue;
                }
                
                InstrucaoNavegacao instrucao = new InstrucaoNavegacao();
                
                double duracao = step.get("duration").asDouble();
                
                instrucao.setDistancia(distancia);
                instrucao.setDuracao(duracao);
                instrucao.setDistanciaAcumulada(distanciaAcumulada);
                
                distanciaAcumulada += distancia;
                
                JsonNode maneuver = step.get("maneuver");
                String maneuverType = maneuver.get("type").asText();
                String maneuverModifier = maneuver.has("modifier") ? 
                    maneuver.get("modifier").asText() : "";
                String nomeRua = step.get("name").asText();
                
                // Extrair coordenadas da manobra
                if (maneuver.has("location")) {
                    JsonNode location = maneuver.get("location");
                    instrucao.setLongitude(location.get(0).asDouble());
                    instrucao.setLatitude(location.get(1).asDouble());
                }
                
                if (nomeRua.isEmpty() || nomeRua.equals("")) {
                    nomeRua = "estrada";
                }
                
                instrucao.setTipo(maneuverType);
                instrucao.setDirecao(maneuverModifier);
                instrucao.setNomeRua(nomeRua);
                instrucao.setInstrucao(gerarInstrucaoTexto(maneuverType, maneuverModifier, nomeRua, distancia));
                
                // Calcular distância para alerta (200m antes da manobra)
                instrucao.setDistanciaAlerta(Math.max(0, distanciaAcumulada - 200));
                
                instrucoes.add(instrucao);
            }
            
            // Adicionar instrução de chegada
            if (!instrucoes.isEmpty()) {
                InstrucaoNavegacao chegada = new InstrucaoNavegacao();
                chegada.setTipo("arrive");
                chegada.setInstrucao("Você chegou ao seu destino!");
                chegada.setDistancia(0);
                chegada.setDuracao(0);
                chegada.setDistanciaAcumulada(distanciaAcumulada);
                instrucoes.add(chegada);
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao processar instruções: " + e.getMessage());
            e.printStackTrace();
        }
        
        return instrucoes;
    }
    
    private String gerarInstrucaoTexto(String tipo, String direcao, String rua, double distancia) {
        StringBuilder instrucao = new StringBuilder();
        
        switch (tipo) {
            case "depart":
                instrucao.append("Inicie na ").append(rua);
                break;
            case "arrive":
                instrucao.append("Você chegou ao destino");
                return instrucao.toString();
            case "turn":
                if ("left".equals(direcao)) {
                    instrucao.append("Vire à esquerda");
                } else if ("right".equals(direcao)) {
                    instrucao.append("Vire à direita");
                } else if ("sharp left".equals(direcao)) {
                    instrucao.append("Vire acentuadamente à esquerda");
                } else if ("sharp right".equals(direcao)) {
                    instrucao.append("Vire acentuadamente à direita");
                } else if ("slight left".equals(direcao)) {
                    instrucao.append("Mantenha-se à esquerda");
                } else if ("slight right".equals(direcao)) {
                    instrucao.append("Mantenha-se à direita");
                } else {
                    instrucao.append("Continue");
                }
                if (!rua.equals("estrada")) {
                    instrucao.append(" na ").append(rua);
                }
                break;
            case "continue":
                instrucao.append("Continue na ").append(rua);
                break;
            case "roundabout":
            case "rotary":
                instrucao.append("Entre na rotatória");
                if (!direcao.isEmpty()) {
                    instrucao.append(" e pegue a ").append(direcao).append(" saída");
                }
                break;
            case "fork":
                if ("left".equals(direcao)) {
                    instrucao.append("Na bifurcação, mantenha-se à esquerda");
                } else {
                    instrucao.append("Na bifurcação, mantenha-se à direita");
                }
                break;
            case "merge":
                instrucao.append("Entre na via");
                break;
            case "on ramp":
                instrucao.append("Entre na via expressa");
                break;
            case "off ramp":
                instrucao.append("Saia da via expressa");
                break;
            default:
                instrucao.append("Siga em frente");
                if (!rua.equals("estrada")) {
                    instrucao.append(" na ").append(rua);
                }
        }
        
        // Adicionar distância se for significativa
        if (distancia > 50) {
            instrucao.append(" por ").append(Math.round(distancia)).append(" metros");
        }
        
        return instrucao.toString();
    }
    
    private Endereco criarEnderecoComErro(String mensagem) {
        Endereco enderecoErro = new Endereco();
        enderecoErro.setErro(true);
        return enderecoErro;
    }
}