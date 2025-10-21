package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
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
import java.util.Collections;
import java.util.List;

@Service
public class GeocodingServiceImpl implements GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingServiceImpl.class);
    
    @Value("${app.geocoding.nominatim.url:https://nominatim.openstreetmap.org/search}")
    private String nominatimUrl;
    
    @Value("${app.geocoding.nominatim.format:json}")
    private String format;
    
    @Value("${app.geocoding.nominatim.limit:1}")
    private String limit;
    
    private final CloseableHttpClient httpClient;

    public GeocodingServiceImpl() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public Endereco geocodificar(Endereco endereco) {
        try {
            String enderecoCompleto = construirEnderecoCompleto(endereco);
            return geocodificar(enderecoCompleto);
            
        } catch (Exception e) {
            logger.error("Erro na geocodificação do endereço: {}", endereco, e);
            throw new RuntimeException("Falha na geocodificação: " + e.getMessage(), e);
        }
    }

    @Override
    public Endereco geocodificar(String enderecoCompleto) {
        try {
            logger.info("Geocodificando endereço: {}", enderecoCompleto);
            
            URI uri = new URIBuilder(nominatimUrl)
                    .addParameter("q", enderecoCompleto)
                    .addParameter("format", format)
                    .addParameter("limit", limit)
                    .addParameter("addressdetails", "1")
                    // REMOVIDO: .addParameter("countrycodes", "br") // Agora é global/amplo
                    .addParameter("accept-language", "pt-br")
                    .build();
            
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "SistemaRotas/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                logger.debug("Resposta Nominatim: {}", jsonResponse);
                
                JSONArray results = new JSONArray(jsonResponse);
                
                if (results.length() == 0) {
                    throw new RuntimeException("Endereço não encontrado: " + enderecoCompleto);
                }
                
                JSONObject firstResult = results.getJSONObject(0);
                return parseNominatimResult(firstResult, enderecoCompleto);
            }
            
        } catch (Exception e) {
            logger.error("Erro na geocodificação do endereço: {}", enderecoCompleto, e);
            throw new RuntimeException("Falha na geocodificação: " + e.getMessage(), e);
        }
    }

    @Override
    public Endereco reverseGeocoding(Double latitude, Double longitude) {
        try {
            if (!validarCoordenadas(latitude, longitude)) {
                throw new IllegalArgumentException("Coordenadas inválidas: " + latitude + ", " + longitude);
            }
            
            logger.info("Reverse geocoding para coordenadas: {}, {}", latitude, longitude);
            
            URI uri = new URIBuilder("https://nominatim.openstreetmap.org/reverse")
                    .addParameter("lat", latitude.toString())
                    .addParameter("lon", longitude.toString())
                    .addParameter("format", format)
                    .addParameter("addressdetails", "1")
                    .addParameter("accept-language", "pt-br")
                    .build();
            
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "SistemaRotas/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                logger.debug("Resposta Reverse Geocoding: {}", jsonResponse);
                
                JSONObject result = new JSONObject(jsonResponse);
                return parseReverseGeocodingResult(result);
            }
            
        } catch (Exception e) {
            logger.error("Erro no reverse geocoding para {}, {}", latitude, longitude, e);
            throw new RuntimeException("Falha no reverse geocoding: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Endereco> buscarEnderecos(String query) {
        List<Endereco> enderecos = new ArrayList<>();
        
        try {
            if (query == null || query.trim().isEmpty()) {
                return enderecos;
            }
            
            logger.info("Buscando endereços para: {}", query);
            
            URI uri = new URIBuilder(nominatimUrl)
                    .addParameter("q", query)
                    .addParameter("format", format)
                    .addParameter("limit", "10")
                    .addParameter("addressdetails", "1")
                    // REMOVIDO: .addParameter("countrycodes", "br") 
                    .addParameter("accept-language", "pt-br")
                    .build();
            
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "SistemaRotas/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONArray results = new JSONArray(jsonResponse);
                
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    Endereco endereco = parseNominatimResult(result, query);
                    enderecos.add(endereco);
                }
            }
            
        } catch (Exception e) {
            logger.error("Erro na busca de endereços para: {}", query, e);
        }
        
        return enderecos;
    }

    @Override
    public boolean validarCoordenadas(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180;
    }

    /**
     * Constrói endereço completo a partir do objeto Endereco.
     * REMOVIDO o ", Brasil" fixo.
     */
    private String construirEnderecoCompleto(Endereco endereco) {
        StringBuilder sb = new StringBuilder();
        
        if (endereco.getRua() != null && !endereco.getRua().trim().isEmpty()) {
            sb.append(endereco.getRua().trim());
            if (endereco.getNumero() != null && !endereco.getNumero().trim().isEmpty()) {
                sb.append(", ").append(endereco.getNumero().trim());
            }
        }
        
        if (endereco.getBairro() != null && !endereco.getBairro().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(endereco.getBairro().trim());
        }
        
        if (endereco.getCidade() != null && !endereco.getCidade().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(endereco.getCidade().trim());
        }
        
        if (endereco.getEstado() != null && !endereco.getEstado().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(endereco.getEstado().trim());
        }
        
        return sb.toString();
    }

    /**
     * Parse do resultado do Nominatim para objeto Endereco
     */
    private Endereco parseNominatimResult(JSONObject result, String enderecoOriginal) {
        Endereco endereco = new Endereco();
        
        // Coordenadas
        endereco.setLatitude(Double.parseDouble(result.getString("lat")));
        endereco.setLongitude(Double.parseDouble(result.getString("lon")));
        
        // Detalhes do endereço
        JSONObject address = result.getJSONObject("address");
        
        endereco.setRua(obterValorAddress(address, "road", "street"));
        endereco.setNumero(obterValorAddress(address, "house_number"));
        endereco.setBairro(obterValorAddress(address, "suburb", "neighbourhood", "quarter"));
        endereco.setCidade(obterValorAddress(address, "city", "town", "village", "municipality"));
        endereco.setEstado(obterValorAddress(address, "state"));
        
        // Display name como fallback
        if (endereco.getRua() == null) {
            String displayName = result.optString("display_name", "");
            if (!displayName.isEmpty()) {
                endereco.setRua(displayName.split(",")[0].trim());
            }
        }
        
        logger.info("Endereço geocodificado: {} -> ({}, {})", 
                   enderecoOriginal, endereco.getLatitude(), endereco.getLongitude());
        
        return endereco;
    }

    /**
     * Parse do resultado do reverse geocoding
     */
    private Endereco parseReverseGeocodingResult(JSONObject result) {
        Endereco endereco = new Endereco();
        
        JSONObject address = result.getJSONObject("address");
        
        endereco.setRua(obterValorAddress(address, "road", "street"));
        endereco.setNumero(obterValorAddress(address, "house_number"));
        endereco.setBairro(obterValorAddress(address, "suburb", "neighbourhood", "quarter"));
        endereco.setCidade(obterValorAddress(address, "city", "town", "village", "municipality"));
        endereco.setEstado(obterValorAddress(address, "state"));
        
        // Coordenadas
        endereco.setLatitude(Double.parseDouble(result.getString("lat")));
        endereco.setLongitude(Double.parseDouble(result.getString("lon")));
        
        return endereco;
    }

    /**
     * Obtém valor do address object com fallback para múltiplas chaves
     */
    private String obterValorAddress(JSONObject address, String... chaves) {
        for (String chave : chaves) {
            if (address.has(chave)) {
                return address.getString(chave);
            }
        }
        return null;
    }
}