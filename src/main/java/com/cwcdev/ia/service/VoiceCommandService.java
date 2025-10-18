package com.cwcdev.ia.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class VoiceCommandService {
    
    // CORREÇÃO: Substituir Map.of() por HashMap tradicional
    private static final Map<String, String> COMANDOS_VOZ;
    private static final Map<String, String> SINONIMOS_CIDADES;
    
    static {
        // Inicialização estática para Java 8
        Map<String, String> comandosTemp = new HashMap<>();
        comandosTemp.put("vou para", "navigate");
        comandosTemp.put("me leve até", "navigate");
        comandosTemp.put("como chegar em", "navigate");
        comandosTemp.put("onde fica", "find");
        comandosTemp.put("encontre", "find");
        comandosTemp.put("parar navegação", "stop");
        comandosTemp.put("cancelar rota", "stop");
        COMANDOS_VOZ = Collections.unmodifiableMap(comandosTemp);
        
        Map<String, String> sinonimosTemp = new HashMap<>();
        sinonimosTemp.put("shopping", "Shopping Vale do Aço");
        sinonimosTemp.put("hospital", "Hospital Municipal");
        sinonimosTemp.put("prefeitura", "Prefeitura Municipal");
        sinonimosTemp.put("centro", "Centro");
        sinonimosTemp.put("rodoviária", "Rodoviária");
        SINONIMOS_CIDADES = Collections.unmodifiableMap(sinonimosTemp);
    }
    
    /**
     * Processa comando de voz e extrai intenção + local
     */
    public ComandoVoz processarComando(String comandoVoz) {
        String comandoLower = comandoVoz.toLowerCase();
        
        // Encontrar intenção
        String intencao = encontrarIntencao(comandoLower);
        
        // Extrair localização
        String localizacao = extrairLocalizacao(comandoLower, intencao);
        
        // Aplicar sinônimos inteligentes
        localizacao = aplicarSinonimos(localizacao);
        
        return new ComandoVoz(intencao, localizacao, comandoVoz);
    }
    
    private String encontrarIntencao(String comando) {
        // CORREÇÃO: Substituir stream() por loop tradicional
        for (Map.Entry<String, String> entry : COMANDOS_VOZ.entrySet()) {
            if (comando.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "unknown";
    }
    
    private String extrairLocalizacao(String comando, String intencao) {
        // Remove a parte do comando para extrair só o local
        String comandoSemIntencao = comando;
        for (String cmd : COMANDOS_VOZ.keySet()) {
            comandoSemIntencao = comandoSemIntencao.replace(cmd, "").trim();
        }
        
        // Remove palavras comuns
        String[] palavrasRemover = {"em", "no", "na", "de", "para", "o", "a"};
        for (String palavra : palavrasRemover) {
            comandoSemIntencao = comandoSemIntencao.replace(" " + palavra + " ", " ");
        }
        
        return comandoSemIntencao.trim();
    }
    
    private String aplicarSinonimos(String localizacao) {
        String localLower = localizacao.toLowerCase();
        // CORREÇÃO: getOrDefault com verificação manual
        if (SINONIMOS_CIDADES.containsKey(localLower)) {
            return SINONIMOS_CIDADES.get(localLower);
        }
        return localizacao;
    }
    
    // Classe para representar o comando processado
    public static class ComandoVoz {
        private String intencao;
        private String localizacao;
        private String comandoOriginal;
        
        public ComandoVoz(String intencao, String localizacao, String comandoOriginal) {
            this.intencao = intencao;
            this.localizacao = localizacao;
            this.comandoOriginal = comandoOriginal;
        }
        
        // getters
        public String getIntencao() { 
            return intencao; 
        }
        
        public String getLocalizacao() { 
            return localizacao; 
        }
        
        public String getComandoOriginal() { 
            return comandoOriginal; 
        }
        
        // setters (opcionais)
        public void setIntencao(String intencao) {
            this.intencao = intencao;
        }
        
        public void setLocalizacao(String localizacao) {
            this.localizacao = localizacao;
        }
        
        public void setComandoOriginal(String comandoOriginal) {
            this.comandoOriginal = comandoOriginal;
        }
        
        @Override
        public String toString() {
            return "ComandoVoz{" +
                   "intencao='" + intencao + '\'' +
                   ", localizacao='" + localizacao + '\'' +
                   ", comandoOriginal='" + comandoOriginal + '\'' +
                   '}';
        }
    }
}