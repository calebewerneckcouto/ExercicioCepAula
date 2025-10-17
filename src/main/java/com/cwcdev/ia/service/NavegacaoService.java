package com.cwcdev.ia.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.InstrucaoNavegacao;
import com.cwcdev.ia.model.Rota;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    /**
     * Busca endereço por CEP ou consulta textual usando Streams Java 8
     */
    public List<Endereco> buscarEnderecos(String query) {
        List<Endereco> resultados = new ArrayList<>();
        
        // Limpar query
        String queryLimpa = query.trim();
        
        // Verificar se é CEP (apenas números, 8 dígitos)
        String apenasNumeros = queryLimpa.replaceAll("[^0-9]", "");
        
        if (apenasNumeros.length() == 8) {
            // É um CEP
            Endereco endereco = buscarEnderecoPorCep(apenasNumeros);
            if (!endereco.isErro()) {
                resultados.add(endereco);
            }
        } else {
            // É uma busca textual - buscar no Nominatim
            resultados = buscarPorTexto(queryLimpa);
        }
        
        return resultados;
    }

    /**
     * Busca por CEP no ViaCEP
     */
    public Endereco buscarEnderecoPorCep(String cep) {
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
                return endereco;
            }
            
            if (endereco != null && !endereco.isErro()) {
                buscarCoordenadas(endereco);
            }
            
            return endereco;
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar CEP: " + e.getMessage());
            return criarEnderecoComErro("Erro na busca do CEP");
        }
    }

    /**
     * Busca textual usando Nominatim com filtro para Brasil
     */
    public List<Endereco> buscarPorTexto(String query) {
        try {
            String url = String.format("%s?format=json&q=%s&addressdetails=1&limit=10&countrycodes=br", 
                NOMINATIM_URL, 
                java.net.URLEncoder.encode(query, "UTF-8"));
            
            // Configurar User-Agent
            restTemplate.getInterceptors().clear();
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("User-Agent", "GPS-Navegacao-App/1.0");
                return execution.execute(request, body);
            });
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Usar Streams Java 8 para processar resultados
            if (root.isArray()) {
                return StreamSupport.stream(root.spliterator(), false)
                    .map(this::jsonNodeParaEndereco)
                    .filter(e -> e != null && !e.isErro())
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar por texto: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * Converte JsonNode do Nominatim para Endereco usando Streams
     */
    private Endereco jsonNodeParaEndereco(JsonNode node) {
        try {
            Endereco endereco = new Endereco();
            
            // Extrair coordenadas
            endereco.setLatitude(node.get("lat").asDouble());
            endereco.setLongitude(node.get("lon").asDouble());
            
            // Extrair detalhes do endereço se disponível
            if (node.has("address")) {
                JsonNode address = node.get("address");
                
                // Usar Stream para encontrar o melhor campo para logradouro
                String logradouro = Stream.of("road", "street", "pedestrian", "footway")
                    .filter(address::has)
                    .findFirst()
                    .map(field -> address.get(field).asText())
                    .orElse(node.has("display_name") ? 
                        node.get("display_name").asText().split(",")[0] : "");
                
                endereco.setLogradouro(logradouro);
                
                // Bairro
                if (address.has("suburb")) {
                    endereco.setBairro(address.get("suburb").asText());
                } else if (address.has("neighbourhood")) {
                    endereco.setBairro(address.get("neighbourhood").asText());
                }
                
                // Cidade
                if (address.has("city")) {
                    endereco.setLocalidade(address.get("city").asText());
                } else if (address.has("town")) {
                    endereco.setLocalidade(address.get("town").asText());
                } else if (address.has("municipality")) {
                    endereco.setLocalidade(address.get("municipality").asText());
                }
                
                // Estado
                if (address.has("state")) {
                    String estado = address.get("state").asText();
                    // Converter nome completo para sigla se necessário
                    endereco.setUf(converterEstadoParaSigla(estado));
                }
                
                // CEP se disponível
                if (address.has("postcode")) {
                    endereco.setCep(address.get("postcode").asText());
                } else {
                    endereco.setCep("N/A");
                }
            }
            
            endereco.setErro(false);
            return endereco;
            
        } catch (Exception e) {
            System.err.println("Erro ao converter JsonNode: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converte nome de estado para sigla
     */
    private String converterEstadoParaSigla(String estado) {
        // Map de estados para siglas usando Java 8
        java.util.Map<String, String> estadosMap = new java.util.HashMap<>();
        estadosMap.put("Acre", "AC");
        estadosMap.put("Alagoas", "AL");
        estadosMap.put("Amapá", "AP");
        estadosMap.put("Amazonas", "AM");
        estadosMap.put("Bahia", "BA");
        estadosMap.put("Ceará", "CE");
        estadosMap.put("Distrito Federal", "DF");
        estadosMap.put("Espírito Santo", "ES");
        estadosMap.put("Goiás", "GO");
        estadosMap.put("Maranhão", "MA");
        estadosMap.put("Mato Grosso", "MT");
        estadosMap.put("Mato Grosso do Sul", "MS");
        estadosMap.put("Minas Gerais", "MG");
        estadosMap.put("Pará", "PA");
        estadosMap.put("Paraíba", "PB");
        estadosMap.put("Paraná", "PR");
        estadosMap.put("Pernambuco", "PE");
        estadosMap.put("Piauí", "PI");
        estadosMap.put("Rio de Janeiro", "RJ");
        estadosMap.put("Rio Grande do Norte", "RN");
        estadosMap.put("Rio Grande do Sul", "RS");
        estadosMap.put("Rondônia", "RO");
        estadosMap.put("Roraima", "RR");
        estadosMap.put("Santa Catarina", "SC");
        estadosMap.put("São Paulo", "SP");
        estadosMap.put("Sergipe", "SE");
        estadosMap.put("Tocantins", "TO");
        
        return estadosMap.getOrDefault(estado, estado.length() <= 2 ? estado : "BR");
    }

    /**
     * Busca coordenadas de um endereço usando Nominatim
     */
    private boolean buscarCoordenadas(Endereco endereco) {
        try {
            String enderecoCompleto = construirEnderecoCompleto(endereco);
            
            String url = String.format("%s?format=json&q=%s&limit=1&countrycodes=br", 
                NOMINATIM_URL, 
                java.net.URLEncoder.encode(enderecoCompleto, "UTF-8"));
            
            restTemplate.getInterceptors().clear();
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

    /**
     * Constrói endereço completo usando Streams
     */
    private String construirEnderecoCompleto(Endereco endereco) {
        return Stream.of(
                endereco.getLogradouro(),
                endereco.getBairro(),
                endereco.getLocalidade(),
                endereco.getUf()
            )
            .filter(s -> s != null && !s.isEmpty())
            .collect(Collectors.joining(", "));
    }

    /**
     * Calcula rota entre origem e destino
     */
    public Rota calcularRota(Endereco origem, Endereco destino) {
        try {
            // Garantir coordenadas
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
            
            System.out.println("📍 Origem: " + origem.getLatitude() + ", " + origem.getLongitude());
            System.out.println("🏁 Destino: " + destino.getLatitude() + ", " + destino.getLongitude());
            
            String coordenadas = String.format("%s,%s;%s,%s", 
                origem.getLongitude(), origem.getLatitude(),
                destino.getLongitude(), destino.getLatitude());
            
            String url = OSRM_URL + coordenadas + "?overview=full&steps=true&geometries=polyline&annotations=true";
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.get("code").asText().equals("Ok")) {
                JsonNode route = root.get("routes").get(0);
                
                Rota rota = new Rota();
                rota.setOrigem(origem);
                rota.setDestino(destino);
                rota.setDistancia(route.get("distance").asDouble());
                rota.setDuracao(route.get("duration").asDouble());
                rota.setDadosRotaCompleta(root);
                
                // Processar instruções usando Streams
                List<InstrucaoNavegacao> instrucoes = processarInstrucoesComStreams(route);
                rota.setInstrucoes(instrucoes);
                
                System.out.println("✓ Rota: " + String.format("%.1f km", rota.getDistancia() / 1000) + 
                                 " - " + String.format("%.0f min", rota.getDuracao() / 60));
                
                return rota;
            } else {
                throw new RuntimeException("Erro OSRM: " + root.get("message").asText());
            }
            
        } catch (Exception e) {
            System.err.println("✗ Erro ao calcular rota: " + e.getMessage());
            throw new RuntimeException("Erro ao calcular rota: " + e.getMessage());
        }
    }
    
    /**
     * Processa instruções de navegação usando Streams Java 8
     */
    private List<InstrucaoNavegacao> processarInstrucoesComStreams(JsonNode route) {
        try {
            JsonNode steps = route.get("legs").get(0).get("steps");
            
            final double[] distanciaAcumulada = {0.0}; // Array para usar em lambda
            
            // Converter steps em Stream e processar
            List<InstrucaoNavegacao> instrucoes = StreamSupport.stream(steps.spliterator(), false)
                .filter(step -> step.get("distance").asDouble() >= 10) // Filtrar instruções muito curtas
                .map(step -> {
                    InstrucaoNavegacao instrucao = new InstrucaoNavegacao();
                    
                    double distancia = step.get("distance").asDouble();
                    double duracao = step.get("duration").asDouble();
                    
                    instrucao.setDistancia(distancia);
                    instrucao.setDuracao(duracao);
                    instrucao.setDistanciaAcumulada(distanciaAcumulada[0]);
                    
                    distanciaAcumulada[0] += distancia;
                    
                    JsonNode maneuver = step.get("maneuver");
                    String tipo = maneuver.get("type").asText();
                    String direcao = maneuver.has("modifier") ? maneuver.get("modifier").asText() : "";
                    String nomeRua = step.get("name").asText();
                    
                    // Coordenadas da manobra
                    if (maneuver.has("location")) {
                        JsonNode location = maneuver.get("location");
                        instrucao.setLongitude(location.get(0).asDouble());
                        instrucao.setLatitude(location.get(1).asDouble());
                    }
                    
                    if (nomeRua == null || nomeRua.isEmpty()) {
                        nomeRua = "estrada";
                    }
                    
                    instrucao.setTipo(tipo);
                    instrucao.setDirecao(direcao);
                    instrucao.setNomeRua(nomeRua);
                    instrucao.setInstrucao(gerarInstrucaoTexto(tipo, direcao, nomeRua, distancia));
                    instrucao.setDistanciaAlerta(Math.max(0, distanciaAcumulada[0] - 200));
                    
                    return instrucao;
                })
                .collect(Collectors.toList());
            
            // Adicionar instrução de chegada
            if (!instrucoes.isEmpty()) {
                InstrucaoNavegacao chegada = new InstrucaoNavegacao();
                chegada.setTipo("arrive");
                chegada.setInstrucao("Você chegou ao seu destino!");
                chegada.setDistancia(0);
                chegada.setDuracao(0);
                chegada.setDistanciaAcumulada(distanciaAcumulada[0]);
                instrucoes.add(chegada);
            }
            
            return instrucoes;
            
        } catch (Exception e) {
            System.err.println("Erro ao processar instruções: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gera texto de instrução de navegação
     */
    private String gerarInstrucaoTexto(String tipo, String direcao, String rua, double distancia) {
        StringBuilder instrucao = new StringBuilder();
        
        switch (tipo) {
            case "depart":
                instrucao.append("Inicie na ").append(rua);
                break;
            case "arrive":
                return "Você chegou ao destino";
            case "turn":
                instrucao.append(getTurnText(direcao));
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
                instrucao.append(direcao.equals("left") ? 
                    "Na bifurcação, mantenha-se à esquerda" : 
                    "Na bifurcação, mantenha-se à direita");
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
        
        // Adicionar distância se significativa
        if (distancia > 50) {
            instrucao.append(" por ").append(Math.round(distancia)).append(" metros");
        }
        
        return instrucao.toString();
    }
    
    private String getTurnText(String direcao) {
        switch (direcao) {
            case "left": return "Vire à esquerda";
            case "right": return "Vire à direita";
            case "sharp left": return "Vire acentuadamente à esquerda";
            case "sharp right": return "Vire acentuadamente à direita";
            case "slight left": return "Mantenha-se à esquerda";
            case "slight right": return "Mantenha-se à direita";
            default: return "Continue";
        }
    }
    
    private Endereco criarEnderecoComErro(String mensagem) {
        Endereco enderecoErro = new Endereco();
        enderecoErro.setErro(true);
        return enderecoErro;
    }
}