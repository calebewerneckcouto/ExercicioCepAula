package com.cwcdev.ia.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.repository.RotaRepository;

@Service
public class RecommendationService {
    
    @Autowired
    private RotaRepository rotaRepository;
    
    @Autowired
    private GeocodingService geocodingService;
    
    private Map<String, Integer> historicoBusca = new HashMap<>();
    private Map<String, PreferenciaUsuario> preferencias = new HashMap<>();
    
    /**
     * Recomenda destinos baseado no histórico
     */
    public List<Endereco> recomendarDestinos(String sessionId) {
        List<Endereco> recomendacoes = new ArrayList<>();
        
        // 1. Baseado no histórico de buscas
        recomendacoes.addAll(recomendarPorHistorico(sessionId));
        
        // 2. Pontos turísticos locais (Ipatinga)
        recomendacoes.addAll(recomendarPontosTuristicos());
        
        // 3. Baseado no horário do dia
        recomendacoes.addAll(recomendarPorHorario());
        
        return recomendacoes.stream().distinct().limit(5).collect(Collectors.toList());
    }
    
    /**
     * Registra uma busca no histórico
     */
    public void registrarBusca(String sessionId, String termoBusca) {
        historicoBusca.merge(termoBusca.toLowerCase(), 1, Integer::sum);
        
        PreferenciaUsuario pref = preferencias.getOrDefault(sessionId, new PreferenciaUsuario());
        pref.adicionarBusca(termoBusca);
        preferencias.put(sessionId, pref);
    }
    
    private List<Endereco> recomendarPorHistorico(String sessionId) {
        PreferenciaUsuario pref = preferencias.get(sessionId);
        if (pref == null) return new ArrayList<>();
        
        return pref.getUltimasBuscas().stream()
            .map(this::criarEnderecoFromBusca)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private List<Endereco> recomendarPontosTuristicos() {
        List<Endereco> pontos = new ArrayList<>();
        
        // Pontos turísticos de Ipatinga
        pontos.add(criarEndereco("Parque Ipanema", "Ipatinga", "MG"));
        pontos.add(criarEndereco("Shopping Vale do Aço", "Ipatinga", "MG"));
        pontos.add(criarEndereco("Estádio Municipal João Lamego", "Ipatinga", "MG"));
        pontos.add(criarEndereco("Praça 1º de Maio", "Centro", "Ipatinga", "MG"));
        pontos.add(criarEndereco("Cachoeira do Tabuleiro", "Timóteo", "MG"));
        
        return pontos;
    }
    
    private List<Endereco> recomendarPorHorario() {
        LocalTime agora = LocalTime.now();
        List<Endereco> recomendacoes = new ArrayList<>();
        
        if (agora.isAfter(LocalTime.of(6, 0)) && agora.isBefore(LocalTime.of(10, 0))) {
            // Manhã: cafés, padarias
            recomendacoes.add(criarEndereco("Padaria Pão Quente", "Centro", "Ipatinga", "MG"));
        }
        
        if (agora.isAfter(LocalTime.of(11, 0)) && agora.isBefore(LocalTime.of(14, 0))) {
            // Almoço: restaurantes
            recomendacoes.add(criarEndereco("Restaurante Sabor Mineiro", "Centro", "Ipatinga", "MG"));
        }
        
        if (agora.isAfter(LocalTime.of(17, 0)) && agora.isBefore(LocalTime.of(22, 0))) {
            // Noite: bares, shoppings
            recomendacoes.add(criarEndereco("Shopping Vale do Aço", "Ipatinga", "MG"));
        }
        
        return recomendacoes;
    }
    
    private Endereco criarEnderecoFromBusca(String busca) {
        try {
            // Tenta geocodificar a busca anterior
            return geocodingService.geocodificar(busca + ", Ipatinga, MG");
        } catch (Exception e) {
            return null;
        }
    }
    
    private Endereco criarEndereco(String rua, String cidade, String estado) {
        Endereco endereco = new Endereco();
        endereco.setRua(rua);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        return endereco;
    }
    
    private Endereco criarEndereco(String rua, String bairro, String cidade, String estado) {
        Endereco endereco = new Endereco();
        endereco.setRua(rua);
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        return endereco;
    }
    
    // Classe para preferências do usuário
    private static class PreferenciaUsuario {
        private List<String> ultimasBuscas = new ArrayList<>();
        private static final int MAX_HISTORICO = 10;
        
        public void adicionarBusca(String busca) {
            ultimasBuscas.add(0, busca); // Adiciona no início
            if (ultimasBuscas.size() > MAX_HISTORICO) {
                ultimasBuscas = ultimasBuscas.subList(0, MAX_HISTORICO);
            }
        }
        
        public List<String> getUltimasBuscas() {
            return new ArrayList<>(ultimasBuscas);
        }
    }
}