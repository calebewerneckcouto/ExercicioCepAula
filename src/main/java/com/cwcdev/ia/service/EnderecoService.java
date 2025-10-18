package com.cwcdev.ia.service;

import java.util.List;
import java.util.Optional;

import com.cwcdev.ia.model.Endereco;

public interface EnderecoService {
    
    /**
     * Salva um endereço e realiza geocodificação se necessário
     */
    Endereco salvarEndereco(Endereco endereco);
    
    /**
     * Busca endereço por ID
     */
    Optional<Endereco> buscarEnderecoPorId(Long id);
    
    /**
     * Busca todos os endereços
     */
    List<Endereco> buscarTodosEnderecos();
    
    /**
     * Busca endereços por cidade
     */
    List<Endereco> buscarEnderecosPorCidade(String cidade);
    
    /**
     * Busca endereços por estado
     */
    List<Endereco> buscarEnderecosPorEstado(String estado);
    
    /**
     * Atualiza um endereço existente
     */
    Endereco atualizarEndereco(Long id, Endereco enderecoAtualizado);
    
    /**
     * Exclui um endereço por ID
     */
    void excluirEndereco(Long id);
    
    /**
     * Realiza geocodificação de um endereço (objeto)
     */
    Endereco geocodificarEndereco(Endereco endereco);
    
    /**
     * NOVO: Geocodificar a partir de string completa
     */
    Endereco geocodificar(String enderecoCompleto);
    
    /**
     * NOVO: Buscar múltiplos endereços (autocomplete)
     */
    List<Endereco> buscarEnderecos(String query);
    
    /**
     * NOVO: Reverse geocoding (coordenadas → endereço)
     */
    Endereco reverseGeocoding(Double latitude, Double longitude);
    
    /**
     * Busca endereço por coordenadas
     */
    Optional<Endereco> buscarEnderecoPorCoordenadas(Double latitude, Double longitude);
    
    /**
     * Verifica se endereço já existe no banco
     */
    boolean enderecoExiste(Endereco endereco);
    
    /**
     * Formata endereço completo para string
     */
    String formatarEnderecoCompleto(Endereco endereco);
}