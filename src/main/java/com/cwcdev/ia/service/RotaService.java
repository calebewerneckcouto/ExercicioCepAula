package com.cwcdev.ia.service;

import java.util.List;
import java.util.Optional;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;

public interface RotaService {
    
    /**
     * Calcula rota entre dois endereços
     */
    Rota calcularRota(Endereco origem, Endereco destino);
    
    /**
     * Calcula rota por IDs dos endereços
     */
    Rota calcularRota(Long origemId, Long destinoId);
    
    /**
     * Salva rota no banco de dados
     */
    Rota salvarRota(Rota rota);
    
    /**
     * Busca rota por ID - RETORNA Optional
     */
    Optional<Rota> buscarRotaPorId(Long id);
    
    /**
     * Busca todas as rotas
     */
    List<Rota> buscarTodasRotas();
    
    /**
     * Busca rotas por endereço de origem
     */
    List<Rota> buscarRotasPorOrigem(Long enderecoOrigemId);
    
    /**
     * Busca rotas por endereço de destino
     */
    List<Rota> buscarRotasPorDestino(Long enderecoDestinoId);
    
    /**
     * Atualiza uma rota existente
     */
    Rota atualizarRota(Long id, Rota rotaAtualizada);
    
    /**
     * Exclui uma rota
     */
    void excluirRota(Long id);
    
    /**
     * Verifica se rota já existe
     */
    boolean rotaExiste(Long origemId, Long destinoId);
    
    /**
     * Busca rota específica por origem e destino
     */
    Optional<Rota> buscarRotaPorOrigemEDestino(Long origemId, Long destinoId);
    
    /**
     * NOVO MÉTODO: Busca rota por ID lançando exceção se não encontrada
     */
    Rota buscarRotaPorIdOuFalhar(Long id);
}