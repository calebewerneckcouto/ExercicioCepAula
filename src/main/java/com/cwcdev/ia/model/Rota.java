package com.cwcdev.ia.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rotas")
public class Rota {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "origem_id", nullable = false)
    private Endereco origem;
    
    @ManyToOne
    @JoinColumn(name = "destino_id", nullable = false)
    private Endereco destino;
    
    @Column(name = "distancia_km")
    private Double distanciaKm;
    
    @Column(name = "duracao_minutos")
    private Double duracaoMinutos;
    
    @Column(name = "polyline", columnDefinition = "TEXT")
    private String polyline; // Para desenhar a rota no mapa
    
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    
    // Construtores
    public Rota() {
        this.dataCriacao = LocalDateTime.now();
    }
    
    public Rota(Endereco origem, Endereco destino) {
        this();
        this.origem = origem;
        this.destino = destino;
    }
    
    public Rota(Endereco origem, Endereco destino, Double distanciaKm, Double duracaoMinutos) {
        this(origem, destino);
        this.distanciaKm = distanciaKm;
        this.duracaoMinutos = duracaoMinutos;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Endereco getOrigem() {
        return origem;
    }
    
    public void setOrigem(Endereco origem) {
        this.origem = origem;
    }
    
    public Endereco getDestino() {
        return destino;
    }
    
    public void setDestino(Endereco destino) {
        this.destino = destino;
    }
    
    public Double getDistanciaKm() {
        return distanciaKm;
    }
    
    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }
    
    public Double getDuracaoMinutos() {
        return duracaoMinutos;
    }
    
    public void setDuracaoMinutos(Double duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }
    
    public String getPolyline() {
        return polyline;
    }
    
    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    // Métodos utilitários
    public String getTempoFormatado() {
        if (duracaoMinutos == null) return "N/A";
        
        int horas = (int) (duracaoMinutos / 60);
        int minutos = (int) (duracaoMinutos % 60);
        
        if (horas > 0) {
            return String.format("%d h %d min", horas, minutos);
        } else {
            return String.format("%d min", minutos);
        }
    }
    
    public String getDistanciaFormatada() {
        if (distanciaKm == null) return "N/A";
        return String.format("%.2f km", distanciaKm);
    }
    
    @Override
    public String toString() {
        return String.format("Rota [%s -> %s] - %s - %s", 
            origem != null ? origem.getCidade() : "N/A",
            destino != null ? destino.getCidade() : "N/A",
            getDistanciaFormatada(),
            getTempoFormatado());
    }
}