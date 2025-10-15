package com.cwcdev.ia.model;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class Rota {
    private Endereco origem;
    private Endereco destino;
    private List<InstrucaoNavegacao> instrucoes;
    private double distancia;
    private double duracao;
    private String geometria; // GeoJSON da rota
    private JsonNode dadosRotaCompleta; // Dados completos da rota
    
    // Getters e Setters
    public Endereco getOrigem() { return origem; }
    public void setOrigem(Endereco origem) { this.origem = origem; }
    
    public Endereco getDestino() { return destino; }
    public void setDestino(Endereco destino) { this.destino = destino; }
    
    public List<InstrucaoNavegacao> getInstrucoes() { return instrucoes; }
    public void setInstrucoes(List<InstrucaoNavegacao> instrucoes) { this.instrucoes = instrucoes; }
    
    public double getDistancia() { return distancia; }
    public void setDistancia(double distancia) { this.distancia = distancia; }
    
    public double getDuracao() { return duracao; }
    public void setDuracao(double duracao) { this.duracao = duracao; }
    
    public String getGeometria() { return geometria; }
    public void setGeometria(String geometria) { this.geometria = geometria; }
    
    public JsonNode getDadosRotaCompleta() { return dadosRotaCompleta; }
    public void setDadosRotaCompleta(JsonNode dadosRotaCompleta) { this.dadosRotaCompleta = dadosRotaCompleta; }
}