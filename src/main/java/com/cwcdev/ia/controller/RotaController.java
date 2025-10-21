package com.cwcdev.ia.controller;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.service.EnderecoService;
import com.cwcdev.ia.service.RotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Importação correta do Spring
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rotas")
@CrossOrigin(origins = "*")
public class RotaController {
    
    @Autowired
    private RotaService rotaService;
    
    @Autowired
    private EnderecoService enderecoService;
    
    @PostMapping("/calcular")
    public ResponseEntity<?> calcularRota(@RequestBody Map<String, Object> request) {
        try {
            // Utilizando casts seguros, assumindo que a entrada do JSON é Map<String, String>
            @SuppressWarnings("unchecked")
            Map<String, String> origemMap = (Map<String, String>) request.get("origem");
            @SuppressWarnings("unchecked")
            Map<String, String> destinoMap = (Map<String, String>) request.get("destino");
            
            // Validação básica de entrada
            if (origemMap == null || destinoMap == null) {
                throw new IllegalArgumentException("Origem e destino são obrigatórios no corpo da requisição.");
            }
            
            // Criar endereço de origem
            Endereco origem = new Endereco();
            origem.setRua(origemMap.get("rua"));
            origem.setNumero(origemMap.get("numero"));
            origem.setBairro(origemMap.get("bairro"));
            origem.setCidade(origemMap.get("cidade"));
            origem.setEstado(origemMap.get("estado"));
            
            // Criar endereço de destino
            Endereco destino = new Endereco();
            destino.setRua(destinoMap.get("rua"));
            destino.setNumero(destinoMap.get("numero"));
            destino.setBairro(destinoMap.get("bairro"));
            destino.setCidade(destinoMap.get("cidade"));
            destino.setEstado(destinoMap.get("estado"));
            
            // Os serviços abaixo garantirão a geocodificação e o cálculo sem restrição de país/município
            origem = enderecoService.salvarEndereco(origem);
            destino = enderecoService.salvarEndereco(destino);
            
            // Calcular rota
            Rota rota = rotaService.calcularRota(origem, destino);
            rota = rotaService.salvarRota(rota);
            
            Map<String, Object> response = new HashMap<>();
            response.put("rota", rota);
            response.put("origem", origem);
            response.put("destino", destino);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Dados inválidos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            // MELHORIA: Usando a constante do Spring HttpStatus.BAD_REQUEST
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Falha no cálculo da rota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Rota>> listarRotas() {
        return ResponseEntity.ok(rotaService.buscarTodasRotas());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Rota> buscarRotaPorId(@PathVariable Long id) {
        try {
            Optional<Rota> rotaOptional = rotaService.buscarRotaPorId(id);
            
            if (rotaOptional.isPresent()) {
                return ResponseEntity.ok(rotaOptional.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (RuntimeException e) {
            // Em caso de falha de serviço ou de banco de dados, ainda retorna 404 para o cliente
            return ResponseEntity.notFound().build();
        }
    }
}