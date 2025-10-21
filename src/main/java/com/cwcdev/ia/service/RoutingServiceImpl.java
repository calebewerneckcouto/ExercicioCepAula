package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingServiceImpl implements RoutingService {

    private static final Logger logger = LoggerFactory.getLogger(RoutingServiceImpl.class);
    
    @Value("${app.routing.osrm.url:http://router.project-osrm.org/route/v1/driving}")
    private String osrmUrl;
    
    private final CloseableHttpClient httpClient;

    public RoutingServiceImpl() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public Rota calcularRota(Endereco origem, Endereco destino) {
        try {
            validarEnderecosParaRota(origem, destino);
            
            return calcularRota(
                origem.getLatitude(), origem.getLongitude(),
                destino.getLatitude(), destino.getLongitude()
            );
            
        } catch (Exception e) {
            logger.error("Erro ao calcular rota entre {} e {}", origem, destino, e);
            throw new RuntimeException("Falha no cálculo da rota: " + e.getMessage(), e);
        }
    }

    @Override
    public Rota calcularRota(Double origLat, Double origLon, Double destLat, Double destLon) {
        try {
            logger.info("Calculando rota: ({}, {}) -> ({}, {})", origLat, origLon, destLat, destLon);
            
            String coordinates = String.format("%s,%s;%s,%s", origLon, origLat, destLon, destLat);
            
            URI uri = new URIBuilder(osrmUrl + "/" + coordinates)
                    .addParameter("overview", "full")
                    .addParameter("geometries", "geojson")
                    .addParameter("steps", "false")
                    .build();
            
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "SistemaRotas/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                logger.debug("Resposta OSRM: {}", jsonResponse);
                
                JSONObject result = new JSONObject(jsonResponse);
                
                if (!result.getString("code").equals("Ok")) {
                    String errorMessage = result.optString("message", "Unknown error");
                    throw new RuntimeException("Erro no OSRM: " + errorMessage);
                }
                
                return parseOsrmResponse(result, origLat, origLon, destLat, destLon);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao calcular rota para coordenadas", e);
            throw new RuntimeException("Falha no cálculo da rota: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Double>> calcularMatrizDistancias(List<Endereco> enderecos) {
        List<List<Double>> matriz = new ArrayList<>();
        
        try {
            if (enderecos == null || enderecos.size() < 2) {
                return matriz;
            }
            
            // Construir string de coordenadas
            StringBuilder coordsBuilder = new StringBuilder();
            for (Endereco endereco : enderecos) {
                if (coordsBuilder.length() > 0) {
                    coordsBuilder.append(";");
                }
                coordsBuilder.append(endereco.getLongitude())
                            .append(",")
                            .append(endereco.getLatitude());
            }
            
            URI uri = new URIBuilder("http://router.project-osrm.org/table/v1/driving/" + coordsBuilder)
                    .addParameter("sources", construirIndices(enderecos.size()))
                    .addParameter("destinations", construirIndices(enderecos.size()))
                    .build();
            
            HttpGet request = new HttpGet(uri);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(jsonResponse);
                
                JSONArray durations = result.getJSONArray("durations");
                
                for (int i = 0; i < durations.length(); i++) {
                    JSONArray linha = durations.getJSONArray(i);
                    List<Double> linhaMatriz = new ArrayList<>();
                    
                    for (int j = 0; j < linha.length(); j++) {
                        linhaMatriz.add(linha.getDouble(j) / 60.0); // Converter para minutos
                    }
                    
                    matriz.add(linhaMatriz);
                }
            }
            
        } catch (Exception e) {
            logger.error("Erro ao calcular matriz de distâncias", e);
        }
        
        return matriz;
    }

    @Override
    public Rota calcularRotaComWaypoints(List<Endereco> waypoints) {
        try {
            if (waypoints == null || waypoints.size() < 2) {
                throw new IllegalArgumentException("São necessários pelo menos 2 waypoints");
            }
            
            // Construir string de coordenadas
            StringBuilder coordsBuilder = new StringBuilder();
            for (Endereco endereco : waypoints) {
                if (coordsBuilder.length() > 0) {
                    coordsBuilder.append(";");
                }
                coordsBuilder.append(endereco.getLongitude())
                            .append(",")
                            .append(endereco.getLatitude());
            }
            
            URI uri = new URIBuilder(osrmUrl + "/" + coordsBuilder)
                    .addParameter("overview", "full")
                    .addParameter("geometries", "geojson")
                    .addParameter("steps", "false")
                    .build();
            
            HttpGet request = new HttpGet(uri);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(jsonResponse);
                
                if (!result.getString("code").equals("Ok")) {
                    String errorMessage = result.optString("message", "Unknown error");
                    throw new RuntimeException("Erro no OSRM: " + errorMessage);
                }
                
                return parseOsrmResponseComWaypoints(result, waypoints);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao calcular rota com waypoints", e);
            throw new RuntimeException("Falha no cálculo da rota com waypoints: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validarCobertura(Endereco endereco) {
        // MUDANÇA: Validação de coordenadas válida globalmente
        Double lat = endereco.getLatitude();
        Double lon = endereco.getLongitude();
        
        return lat != null && lon != null && 
               lat >= -90.0 && lat <= 90.0 && 
               lon >= -180.0 && lon <= 180.0;
    }

    /**
     * Parse da resposta do OSRM para objeto Rota
     */
    private Rota parseOsrmResponse(JSONObject result, Double origLat, Double origLon, Double destLat, Double destLon) {
        JSONArray routes = result.getJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        
        Rota rota = new Rota();
        
        // Distância em metros → km
        double distanciaMetros = route.getDouble("distance");
        rota.setDistanciaKm(distanciaMetros / 1000.0);
        
        // Duração em segundos → minutos
        double duracaoSegundos = route.getDouble("duration");
        rota.setDuracaoMinutos(duracaoSegundos / 60.0);
        
        // Geometria da rota (GeoJSON)
        if (route.has("geometry")) {
            rota.setPolyline(route.getJSONObject("geometry").toString());
        }
        
        logger.info("Rota calculada: {} km, {} min", 
                   String.format("%.2f", rota.getDistanciaKm()),
                   String.format("%.1f", rota.getDuracaoMinutos()));
        
        return rota;
    }

    /**
     * Parse da resposta do OSRM para rota com múltiplos waypoints
     */
    private Rota parseOsrmResponseComWaypoints(JSONObject result, List<Endereco> waypoints) {
        JSONArray routes = result.getJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        
        Rota rota = new Rota();
        rota.setOrigem(waypoints.get(0));
        rota.setDestino(waypoints.get(waypoints.size() - 1));
        
        // Distância e duração totais
        double distanciaMetros = route.getDouble("distance");
        rota.setDistanciaKm(distanciaMetros / 1000.0);
        
        double duracaoSegundos = route.getDouble("duration");
        rota.setDuracaoMinutos(duracaoSegundos / 60.0);
        
        // Geometria da rota
        if (route.has("geometry")) {
            rota.setPolyline(route.getJSONObject("geometry").toString());
        }
        
        return rota;
    }

    /**
     * Valida endereços para cálculo de rota
     */
    private void validarEnderecosParaRota(Endereco origem, Endereco destino) {
        if (origem == null || destino == null) {
            throw new IllegalArgumentException("Endereço de origem e destino são obrigatórios");
        }
        
        if (origem.getLatitude() == null || origem.getLongitude() == null ||
            destino.getLatitude() == null || destino.getLongitude() == null) {
            throw new IllegalArgumentException("Endereços devem ter coordenadas válidas");
        }
        
        if (!validarCobertura(origem) || !validarCobertura(destino)) {
            throw new IllegalArgumentException("Coordenadas inválidas. Verifique os limites de latitude e longitude.");
        }
    }

    /**
     * Constrói string de índices para a API de matriz
     */
    private String construirIndices(int tamanho) {
        StringBuilder indices = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            if (indices.length() > 0) {
                indices.append(";");
            }
            indices.append(i);
        }
        return indices.toString();
    }
}