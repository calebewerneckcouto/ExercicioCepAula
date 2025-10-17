package com.cwcdev.ia.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Endereco {
    
    @JsonProperty("cep")
    private String cep;
    
    @JsonProperty("logradouro")
    private String logradouro;
    
    @JsonProperty("complemento")
    private String complemento;
    
    @JsonProperty("bairro")
    private String bairro;
    
    @JsonProperty("localidade")
    private String localidade;
    
    @JsonProperty("uf")
    private String uf;
    
    @JsonProperty("ibge")
    private String ibge;
    
    @JsonProperty("gia")
    private String gia;
    
    @JsonProperty("ddd")
    private String ddd;
    
    @JsonProperty("siafi")
    private String siafi;
    
    private boolean erro;
    private String mensagemErro; // Novo campo para mensagem de erro
    
    // Novos campos para coordenadas
    private Double latitude;
    private Double longitude;
    
    // Construtores
    public Endereco() {}

    public Endereco(String cep, String logradouro, String complemento, String bairro, 
                   String localidade, String uf, String ibge, String gia, String ddd, String siafi) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.complemento = complemento;
        this.bairro = bairro;
        this.localidade = localidade;
        this.uf = uf;
        this.ibge = ibge;
        this.gia = gia;
        this.ddd = ddd;
        this.siafi = siafi;
        this.erro = false;
        this.mensagemErro = null;
    }

    // Método estático para criar endereço com erro
    public static Endereco criarComErro(String mensagem) {
        Endereco endereco = new Endereco();
        endereco.setErro(true);
        endereco.setMensagemErro(mensagem);
        return endereco;
    }

    // Método para verificar se o endereço é válido
    public boolean isValido() {
        return !erro && cep != null && !cep.trim().isEmpty();
    }

    // Método para obter endereço formatado
    public String getEnderecoFormatado() {
        if (erro) {
            return mensagemErro != null ? mensagemErro : "Endereço não encontrado";
        }
        
        StringBuilder sb = new StringBuilder();
        if (logradouro != null && !logradouro.trim().isEmpty()) {
            sb.append(logradouro);
        }
        if (bairro != null && !bairro.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bairro);
        }
        if (localidade != null && !localidade.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(localidade);
        }
        if (uf != null && !uf.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(uf);
        }
        if (cep != null && !cep.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - CEP: ");
            sb.append(formatarCep(cep));
        }
        
        return sb.length() > 0 ? sb.toString() : "Endereço não disponível";
    }

    // Método para formatar CEP
    private String formatarCep(String cep) {
        if (cep == null || cep.length() != 8) return cep;
        return cep.substring(0, 5) + "-" + cep.substring(5);
    }

    // Getters e Setters
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getLocalidade() { return localidade; }
    public void setLocalidade(String localidade) { this.localidade = localidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getIbge() { return ibge; }
    public void setIbge(String ibge) { this.ibge = ibge; }

    public String getGia() { return gia; }
    public void setGia(String gia) { this.gia = gia; }

    public String getDdd() { return ddd; }
    public void setDdd(String ddd) { this.ddd = ddd; }

    public String getSiafi() { return siafi; }
    public void setSiafi(String siafi) { this.siafi = siafi; }

    public boolean isErro() { return erro; }
    public void setErro(boolean erro) { this.erro = erro; }

    public String getMensagemErro() { return mensagemErro; }
    public void setMensagemErro(String mensagemErro) { this.mensagemErro = mensagemErro; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        if (erro) {
            return "Endereco{erro=true, mensagem='" + mensagemErro + "'}";
        }
        
        return "Endereco{" +
                "cep='" + cep + '\'' +
                ", logradouro='" + logradouro + '\'' +
                ", complemento='" + complemento + '\'' +
                ", bairro='" + bairro + '\'' +
                ", localidade='" + localidade + '\'' +
                ", uf='" + uf + '\'' +
                (latitude != null ? ", latitude=" + latitude : "") +
                (longitude != null ? ", longitude=" + longitude : "") +
                '}';
    }
}