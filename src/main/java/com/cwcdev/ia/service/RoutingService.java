package com.cwcdev.ia.service;

import java.util.List;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;

public interface RoutingService {
    
    /**
     * Calcula rota entre dois endereços
     */
    Rota calcularRota(Endereco origem, Endereco destino);
    
    /**
     * Calcula rota entre coordenadas
     */
    Rota calcularRota(Double origLat, Double origLon, Double destLat, Double destLon);
    
    /**
     * Calcula matriz de distâncias entre múltiplos pontos
     */
    List<List<Double>> calcularMatrizDistancias(List<Endereco> enderecos);
    
    /**
     * Calcula rota com waypoints intermediários
     */
    Rota calcularRotaComWaypoints(List<Endereco> waypoints);
    
    /**
     * Valida se os pontos estão dentro da área de cobertura
     */
    boolean validarCobertura(Endereco endereco);
}