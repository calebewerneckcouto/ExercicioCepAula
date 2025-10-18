package com.cwcdev.ia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cwcdev.ia.model.Endereco;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    
    // Buscar por endereço completo (evitar duplicatas)
    Optional<Endereco> findByRuaAndNumeroAndCidadeAndEstado(
        String rua, String numero, String cidade, String estado);
    
    // Buscar por cidade
    List<Endereco> findByCidadeContainingIgnoreCase(String cidade);
    
    // Buscar por estado
    List<Endereco> findByEstadoContainingIgnoreCase(String estado);
    
    // Buscar por bairro
    List<Endereco> findByBairroContainingIgnoreCase(String bairro);
    
    // Buscar por coordenadas aproximadas
    @Query("SELECT e FROM Endereco e WHERE " +
           "e.latitude BETWEEN :latMin AND :latMax AND " +
           "e.longitude BETWEEN :lonMin AND :lonMax")
    List<Endereco> findByLatitudeBetweenAndLongitudeBetween(
        @Param("latMin") Double latMin,
        @Param("latMax") Double latMax,
        @Param("lonMin") Double lonMin,
        @Param("lonMax") Double lonMax);
    
    // Buscar endereços com coordenadas válidas
    @Query("SELECT e FROM Endereco e WHERE e.latitude IS NOT NULL AND e.longitude IS NOT NULL")
    List<Endereco> findEnderecosComCoordenadas();
}