package com.cwcdev.ia.model;

public class InstrucaoNavegacao {
    private String instrucao;
    private double distancia; // em metros
    private double duracao; // em segundos
    private String tipo; // "depart", "arrive", "turn", "roundabout", etc.
    private String direcao; // "left", "right", "straight", etc.
    private String nomeRua;
    private Double latitude;
    private Double longitude;
    private double distanciaAcumulada; // Distância acumulada até esta instrução
    private double distanciaAlerta; // Distância na qual o alerta deve ser emitido
    private boolean alertaEmitido = false;

    // Construtores
    public InstrucaoNavegacao() {
        this.distanciaAcumulada = 0;
        this.distanciaAlerta = 0;
        this.alertaEmitido = false;
    }

    // Getters e Setters
    public String getInstrucao() { 
        return instrucao; 
    }
    
    public void setInstrucao(String instrucao) { 
        this.instrucao = instrucao; 
    }

    public double getDistancia() { 
        return distancia; 
    }
    
    public void setDistancia(double distancia) { 
        this.distancia = distancia; 
    }

    public double getDuracao() { 
        return duracao; 
    }
    
    public void setDuracao(double duracao) { 
        this.duracao = duracao; 
    }

    public String getTipo() { 
        return tipo; 
    }
    
    public void setTipo(String tipo) { 
        this.tipo = tipo; 
    }

    public String getDirecao() { 
        return direcao; 
    }
    
    public void setDirecao(String direcao) { 
        this.direcao = direcao; 
    }

    public String getNomeRua() { 
        return nomeRua; 
    }
    
    public void setNomeRua(String nomeRua) { 
        this.nomeRua = nomeRua; 
    }

    public Double getLatitude() { 
        return latitude; 
    }
    
    public void setLatitude(Double latitude) { 
        this.latitude = latitude; 
    }

    public Double getLongitude() { 
        return longitude; 
    }
    
    public void setLongitude(Double longitude) { 
        this.longitude = longitude; 
    }

    public double getDistanciaAcumulada() { 
        return distanciaAcumulada; 
    }
    
    public void setDistanciaAcumulada(double distanciaAcumulada) { 
        this.distanciaAcumulada = distanciaAcumulada; 
    }

    public double getDistanciaAlerta() { 
        return distanciaAlerta; 
    }
    
    public void setDistanciaAlerta(double distanciaAlerta) { 
        this.distanciaAlerta = distanciaAlerta; 
    }

    public boolean isAlertaEmitido() { 
        return alertaEmitido; 
    }
    
    public void setAlertaEmitido(boolean alertaEmitido) { 
        this.alertaEmitido = alertaEmitido; 
    }

    @Override
    public String toString() {
        return "InstrucaoNavegacao{" +
                "instrucao='" + instrucao + '\'' +
                ", distancia=" + distancia +
                ", duracao=" + duracao +
                ", tipo='" + tipo + '\'' +
                ", direcao='" + direcao + '\'' +
                ", nomeRua='" + nomeRua + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", distanciaAcumulada=" + distanciaAcumulada +
                '}';
    }
}