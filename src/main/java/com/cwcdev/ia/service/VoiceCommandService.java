package com.cwcdev.ia.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VoiceCommandService {
    
    private static final Map<String, String> COMANDOS_VOZ;
    private static final Map<String, String> SINONIMOS_CIDADES; // Generalizados
    
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
        // Sinônimos Generalizados: o geocoding terá que inferir a localização
        sinonimosTemp.put("shopping", "Shopping");
        sinonimosTemp.put("hospital", "Hospital");
        sinonimosTemp.put("prefeitura", "Prefeitura");
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
        for (Map.Entry<String, String> entry : COMANDOS_VOZ.entrySet()) {
            if (comando.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "unknown";
    }
    
    private String extrairLocalizacao(String comando, String intencao) {
        String comandoSemIntencao = comando;
        for (String cmd : COMANDOS_VOZ.keySet()) {
            comandoSemIntencao = comandoSemIntencao.replace(cmd, "").trim();
        }
        
        // Remove palavras comuns (usando regex para melhor precisão)
        String[] palavrasRemover = {"em", "no", "na", "de", "para", "o", "a"};
        String regexPalavras = "\\b(" + String.join("|", palavrasRemover) + ")\\b";
        
        comandoSemIntencao = comandoSemIntencao.replaceAll(regexPalavras, "").trim();
        comandoSemIntencao = comandoSemIntencao.replaceAll("\\s+", " ");
        
        return comandoSemIntencao.trim();
    }
    
    private String aplicarSinonimos(String localizacao) {
        String localLower = localizacao.toLowerCase();
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
        
        // getters e setters (mantidos)
        public String getIntencao() { return intencao; }
        public String getLocalizacao() { return localizacao; }
        public String getComandoOriginal() { return comandoOriginal; }
        public void setIntencao(String intencao) { this.intencao = intencao; }
        public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
        public void setComandoOriginal(String comandoOriginal) { this.comandoOriginal = comandoOriginal; }
        
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