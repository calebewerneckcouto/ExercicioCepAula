package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class ViaCepService {

    private static final String VIA_CEP_URL = "https://viacep.com.br/ws/";
    
    private final RestTemplate restTemplate;

    // Injeção via construtor (recomendado)
    @Autowired
    public ViaCepService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Endereco buscarEnderecoPorCep(String cep) {
        // Remove caracteres não numéricos
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
            
            // Verifica se o CEP não foi encontrado
            if (endereco != null && endereco.getCep() == null) {
                endereco.setErro(true);
            }
            
            return endereco;
            
        } catch (HttpClientErrorException.NotFound e) {
            // CEP não encontrado
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        } catch (Exception e) {
            // Outros erros
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        }
    }

    public Endereco buscarEnderecoPorLogradouro(String uf, String localidade, String logradouro) {
        try {
            // Codifica os parâmetros para URL
            String ufEncoded = java.net.URLEncoder.encode(uf, "UTF-8");
            String localidadeEncoded = java.net.URLEncoder.encode(localidade, "UTF-8");
            String logradouroEncoded = java.net.URLEncoder.encode(logradouro, "UTF-8");
            
            String url = String.format("%s/%s/%s/%s/json/", 
                VIA_CEP_URL, ufEncoded, localidadeEncoded, logradouroEncoded);
            
            ResponseEntity<Endereco[]> response = restTemplate.getForEntity(url, Endereco[].class);
            
            Endereco[] enderecos = response.getBody();
            if (enderecos != null && enderecos.length > 0) {
                return enderecos[0]; // Retorna o primeiro endereço encontrado
            } else {
                Endereco enderecoErro = new Endereco();
                enderecoErro.setErro(true);
                return enderecoErro;
            }
        } catch (Exception e) {
            Endereco enderecoErro = new Endereco();
            enderecoErro.setErro(true);
            return enderecoErro;
        }
    }
}