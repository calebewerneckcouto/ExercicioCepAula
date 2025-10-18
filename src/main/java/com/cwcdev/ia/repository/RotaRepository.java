package com.cwcdev.ia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long> {
    
    // Buscar rota por origem e destino
    Optional<Rota> findByOrigemAndDestino(Endereco origem, Endereco destino);
    
    // Buscar rotas por endereço de origem
    List<Rota> findByOrigem(Endereco origem);
    
    // Buscar rotas por endereço de destino
    List<Rota> findByDestino(Endereco destino);
    
    // Buscar todas as rotas ordenadas por data de criação
    List<Rota> findAllByOrderByDataCriacaoDesc();
    
    // Buscar rotas com distância maior que
    @Query("SELECT r FROM Rota r WHERE r.distanciaKm > :distanciaMinima")
    List<Rota> findRotasComDistanciaMaiorQue(@Param("distanciaMinima") Double distanciaMinima);
    
    // Buscar rotas com duração menor que
    @Query("SELECT r FROM Rota r WHERE r.duracaoMinutos < :duracaoMaxima")
    List<Rota> findRotasComDuracaoMenorQue(@Param("duracaoMaxima") Double duracaoMaxima);
    
    // Verificar existência de rota entre origem e destino
    boolean existsByOrigemAndDestino(Endereco origem, Endereco destino);
}