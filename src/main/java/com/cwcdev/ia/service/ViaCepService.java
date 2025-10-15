package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ViaCepService {

    private static final String VIA_CEP_URL = "https://viacep.com.br/ws/";
    
    private final RestTemplate restTemplate;

    @Autowired
    public ViaCepService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Endereco buscarEnderecoPorCep(String cep) {
        cep = cep.replaceAll("[^0-9]", "");
        
        if (cep.length() != 8) {
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        }
        
        String url = VIA_CEP_URL + cep + "/json/";
        
        try {
            ResponseEntity<Endereco> response = restTemplate.getForEntity(url, Endereco.class);
            Endereco endereco = response.getBody();
            
            if (endereco != null && endereco.getCep() == null) {
                endereco.setErro(true);
            }
            
            return endereco;
            
        } catch (Exception e) {
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        }
    }

    public Endereco buscarEnderecoPorLogradouro(String uf, String localidade, String logradouro) {
        try {
            uf = uf.trim().toUpperCase();
            localidade = localidade.trim();
            logradouro = logradouro.trim();
            
            // CORREÇÃO: Remove apenas caracteres especiais problemáticos, mantém números
            // Mas remove números no início que podem causar problemas
            logradouro = logradouro.replaceAll("[^a-zA-ZÀ-ÿ0-9\\s\\.\\-]", "");
            logradouro = logradouro.replaceAll("^\\d+\\s*", ""); // Remove números no início
            logradouro = logradouro.replaceAll("\\s+", " ").trim();
            
            if (logradouro.isEmpty()) {
                Endereco enderecoErro = new Endereco();
                enderecoErro.setErro(true);
                return enderecoErro;
            }
            
            String url = VIA_CEP_URL + uf + "/" + localidade + "/" + logradouro + "/json/";
            
            System.out.println("URL da busca: " + url);
            
            ResponseEntity<Endereco[]> response = restTemplate.getForEntity(url, Endereco[].class);
            
            Endereco[] enderecos = response.getBody();
            
            if (enderecos != null && enderecos.length > 0 && enderecos[0].getCep() != null) {
                return enderecos[0];
            } else {
                Endereco enderecoErro = new Endereco();
                enderecoErro.setErro(true);
                return enderecoErro;
            }
                
        } catch (Exception e) {
            System.err.println("Erro na busca por endereço: " + e.getMessage());
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        }
    }}