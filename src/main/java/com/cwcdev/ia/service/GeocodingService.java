package com.cwcdev.ia.service;

import java.util.List;

import com.cwcdev.ia.model.Endereco;

public interface GeocodingService {
    
    /**
     * Geocodifica um endereço (endereço → coordenadas)
     */
    Endereco geocodificar(Endereco endereco);
    
    /**
     * Geocodifica um endereço a partir de string
     */
    Endereco geocodificar(String enderecoCompleto);
    
    /**
     * Reverse geocoding (coordenadas → endereço)
     */
    Endereco reverseGeocoding(Double latitude, Double longitude);
    
    /**
     * Busca múltiplos endereços (para autocomplete)
     */
    List<Endereco> buscarEnderecos(String query);
    
    /**
     * Valida se as coordenadas são válidas
     */
    boolean validarCoordenadas(Double latitude, Double longitude);
}