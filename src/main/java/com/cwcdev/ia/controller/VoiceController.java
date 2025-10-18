package com.cwcdev.ia.controller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.service.EnderecoService;
import com.cwcdev.ia.service.RecommendationService;
import com.cwcdev.ia.service.RotaService;
import com.cwcdev.ia.service.VoiceCommandService;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {
    
    @Autowired
    private VoiceCommandService voiceCommandService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private EnderecoService enderecoService;
    
    @Autowired
    private RotaService rotaService;
    
    @PostMapping("/command")
    public ResponseEntity<?> processarComandoVoz(@RequestBody Map<String, String> request) {
        try {
            String comando = request.get("comando");
            String sessionId = request.getOrDefault("sessionId", "default");
            
            // Processar comando com IA
            VoiceCommandService.ComandoVoz comandoProcessado = 
                voiceCommandService.processarComando(comando);
            
            // Registrar busca para recomendações futuras
            recommendationService.registrarBusca(sessionId, comandoProcessado.getLocalizacao());
            
            Map<String, Object> response = new HashMap<>();
            response.put("comandoProcessado", comandoProcessado);
            
            // Executar ação baseada na intenção
            switch (comandoProcessado.getIntencao()) {
                case "navigate":
                    Endereco destino = enderecoService.geocodificar(comandoProcessado.getLocalizacao());
                    response.put("destino", destino);
                    response.put("acao", "navegar");
                    break;
                    
                case "find":
                    List<Endereco> resultados = buscarLocais(comandoProcessado.getLocalizacao());
                    response.put("resultados", resultados);
                    response.put("acao", "buscar");
                    break;
                    
                case "stop":
                    response.put("acao", "parar");
                    break;
                    
                default:
                    response.put("acao", "desconhecida");
                    response.put("mensagem", "Comando não reconhecido");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Falha ao processar comando: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/recommendations/{sessionId}")
    public ResponseEntity<List<Endereco>> getRecomendacoes(@PathVariable String sessionId) {
        List<Endereco> recomendacoes = recommendationService.recomendarDestinos(sessionId);
        return ResponseEntity.ok(recomendacoes);
    }
    
    private List<Endereco> buscarLocais(String query) {
        try {
            // Busca inteligente por tipo de local
            if (query.toLowerCase().contains("posto") || query.contains("gasolina")) {
                return Arrays.asList(
                    criarEndereco("Posto Ipatinga", "BR-381", "Ipatinga", "MG"),
                    criarEndereco("Posto Shell", "Av. Selim José de Sales", "Ipatinga", "MG")
                );
            } else if (query.toLowerCase().contains("hospital") || query.contains("médico")) {
                return Arrays.asList(
                    criarEndereco("Hospital Municipal", "Av. João Valentim Pascoal", "Ipatinga", "MG"),
                    criarEndereco("Santa Casa", "Rua São Paulo", "Ipatinga", "MG")
                );
            }
            
            // Busca genérica
            return Collections.singletonList(
                enderecoService.geocodificar(query + ", Ipatinga, MG")
            );
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private Endereco criarEndereco(String rua, String numero, String cidade, String estado) {
        Endereco endereco = new Endereco();
        endereco.setRua(rua);
        endereco.setNumero(numero);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        return endereco;
    }
}