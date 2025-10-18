package com.cwcdev.ia.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
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
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.service.EnderecoService;
import com.cwcdev.ia.service.RotaService;

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
            Map<String, String> origemMap = (Map<String, String>) request.get("origem");
            Map<String, String> destinoMap = (Map<String, String>) request.get("destino");
            
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
            
            // Salvar e geocodificar endereços
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
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", e.getMessage());
            return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Rota>> listarRotas() {
        return ResponseEntity.ok(rotaService.buscarTodasRotas());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Rota> buscarRotaPorId(@PathVariable Long id) {
        try {
            // CORREÇÃO: Extrair o Rota do Optional
            Optional<Rota> rotaOptional = rotaService.buscarRotaPorId(id);
            
            if (rotaOptional.isPresent()) {
                return ResponseEntity.ok(rotaOptional.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}