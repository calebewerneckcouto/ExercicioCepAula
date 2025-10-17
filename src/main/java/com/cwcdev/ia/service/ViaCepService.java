package com.cwcdev.ia.service;

import com.cwcdev.ia.model.Endereco;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ViaCepService {

    private static final Logger logger = LoggerFactory.getLogger(ViaCepService.class);
    private static final String VIA_CEP_URL = "https://viacep.com.br/ws/";
    
    private final RestTemplate restTemplate;

    @Autowired
    public ViaCepService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Endereco buscarEnderecoPorCep(String cep) {
        logger.info("Buscando endereço para o CEP: {}", cep);
        
        // Limpa e valida o CEP
        cep = cep.replaceAll("[^0-9]", "");
        
        if (cep.length() != 8) {
            logger.warn("CEP inválido: {}", cep);
            return Endereco.criarComErro("CEP deve conter exatamente 8 dígitos. Formato esperado: 00000000");
        }
        
        String url = VIA_CEP_URL + cep + "/json/";
        logger.debug("URL da requisição: {}", url);
        
        try {
            ResponseEntity<Endereco> response = restTemplate.getForEntity(url, Endereco.class);
            Endereco endereco = response.getBody();
            
            logger.debug("Resposta da API: {}", endereco);
            
            if (endereco == null) {
                logger.warn("Resposta nula da API para CEP: {}", cep);
                return Endereco.criarComErro("Serviço temporariamente indisponível. Tente novamente em alguns instantes.");
            }
            
            // Verifica se o CEP foi encontrado (ViaCEP retorna campo "erro": true quando não encontra)
            if (endereco.getCep() == null || endereco.isErro()) {
                logger.warn("CEP não encontrado: {}", cep);
                return Endereco.criarComErro("CEP " + cep + " não encontrado. Verifique se o CEP está correto.");
            }
            
            logger.info("CEP encontrado com sucesso: {}", cep);
            endereco.setErro(false);
            endereco.setMensagemErro(null);
            return endereco;
            
        } catch (Exception e) {
            logger.error("Erro ao buscar CEP {}: {}", cep, e.getMessage(), e);
            return Endereco.criarComErro("Erro de conexão com o serviço de CEP. Verifique sua internet e tente novamente.");
        }
    }

    public Endereco buscarEnderecoPorLogradouro(String uf, String localidade, String logradouro) {
        logger.info("Buscando endereço por logradouro - UF: {}, Localidade: {}, Logradouro: {}", 
                   uf, localidade, logradouro);
        
        try {
            uf = uf.trim().toUpperCase();
            localidade = localidade.trim();
            logradouro = logradouro.trim();
            
            // Validações básicas
            if (uf.isEmpty() || uf.length() != 2) {
                return Endereco.criarComErro("UF deve conter exatamente 2 caracteres (ex: SP, RJ, MG)");
            }
            
            if (localidade.isEmpty()) {
                return Endereco.criarComErro("Localidade (cidade) é obrigatória");
            }
            
            if (logradouro.isEmpty()) {
                return Endereco.criarComErro("Logradouro (rua/avenida) é obrigatório");
            }
            
            // Limpa o logradouro mantendo caracteres válidos
            logradouro = logradouro.replaceAll("[^a-zA-ZÀ-ÿ0-9\\s\\.\\-]", "");
            logradouro = logradouro.replaceAll("^\\d+\\s*", "");
            logradouro = logradouro.replaceAll("\\s+", " ").trim();
            
            if (logradouro.isEmpty()) {
                return Endereco.criarComErro("Logradouro contém apenas caracteres inválidos");
            }
            
            // Codifica o logradouro para URL
            String logradouroCodificado = java.net.URLEncoder.encode(logradouro, "UTF-8");
            String url = VIA_CEP_URL + uf + "/" + localidade + "/" + logradouroCodificado + "/json/";
            
            logger.debug("URL da busca: {}", url);
            
            ResponseEntity<Endereco[]> response = restTemplate.getForEntity(url, Endereco[].class);
            Endereco[] enderecos = response.getBody();
            
            if (enderecos != null && enderecos.length > 0 && enderecos[0].getCep() != null) {
                logger.info("Endereço encontrado por logradouro: {}", enderecos[0].getCep());
                enderecos[0].setErro(false);
                enderecos[0].setMensagemErro(null);
                return enderecos[0];
            } else {
                logger.warn("Nenhum endereço encontrado para os parâmetros informados");
                return Endereco.criarComErro("Nenhum endereço encontrado para '" + logradouro + 
                                           "' em " + localidade + "/" + uf + 
                                           ". Verifique os dados informados.");
            }
                
        } catch (Exception e) {
            logger.error("Erro na busca por endereço: {}", e.getMessage(), e);
            return Endereco.criarComErro("Erro ao buscar endereço. Tente novamente mais tarde.");
        }
    }
}