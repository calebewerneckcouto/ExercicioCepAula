package com.cwcdev.ia.controller;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.service.EnderecoService;
import com.cwcdev.ia.service.RecommendationService;
import com.cwcdev.ia.service.RotaService;
import com.cwcdev.ia.service.VoiceCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    // Ação "navigate" sempre tenta geocodificar para obter um único destino
                    Endereco destino = enderecoService.geocodificar(comandoProcessado.getLocalizacao());
                    
                    if (destino.getLatitude() != null) {
                        response.put("destino", destino);
                        response.put("acao", "navegar");
                    } else {
                         // Se a geocodificação falhar (sem coordenadas), trata como desconhecido
                        response.put("acao", "desconhecida");
                        response.put("mensagem", "Não foi possível encontrar a localização: " + comandoProcessado.getLocalizacao());
                    }
                    break;
                    
                case "find":
                    // Ação "find" busca múltiplos resultados para um autocompletar/lista
                    List<Endereco> resultados = enderecoService.buscarEnderecos(comandoProcessado.getLocalizacao());
                    response.put("resultados", resultados);
                    response.put("acao", "buscar");
                    break;
                    
                case "stop":
                    response.put("acao", "parar");
                    response.put("mensagem", "Comando de parada recebido.");
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
        // Isso depende da implementação do RecommendationService
        List<Endereco> recomendacoes = recommendationService.recomendarDestinos(sessionId);
        return ResponseEntity.ok(recomendacoes);
    }
    
    
}