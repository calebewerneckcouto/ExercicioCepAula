package com.cwcdev.ia.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.service.EnderecoService;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoController {
    
    @Autowired
    private EnderecoService enderecoService;
    
    @GetMapping
    public List<Endereco> listarEnderecos() {
        return enderecoService.buscarTodosEnderecos();
    }
    
    @PostMapping
    public ResponseEntity<?> salvarEndereco(@RequestBody Endereco endereco) {
        try {
            System.out.println("Recebendo endereco para geocodificação: " + endereco);
            System.out.println("Endereco completo: " + endereco.getEnderecoCompleto());
            
            // Validação do endereço
            if (endereco.getEnderecoCompleto() == null || endereco.getEnderecoCompleto().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Endereço completo é obrigatório");
            }
            
            // Se veio com enderecoCompleto, tenta geocodificar
            Endereco enderecoGeocodificado = enderecoService.geocodificar(endereco.getEnderecoCompleto());
            return ResponseEntity.ok(enderecoGeocodificado);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar endereço: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno: " + e.getMessage());
        }
    }
}