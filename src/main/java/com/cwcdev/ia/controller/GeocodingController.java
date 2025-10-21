package com.cwcdev.ia.controller;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.service.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geocoding")
@CrossOrigin("*")
public class GeocodingController {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingController.class);
    
    @Autowired
    private GeocodingService geocodingService;

    @PostMapping("/geocodificar")
    public ResponseEntity<Endereco> geocodificarEndereco(@RequestBody Endereco endereco) {
        try {
            logger.info("Recebida requisição para geocodificar: {}", endereco);
            
            // Se veio com enderecoCompleto, usa esse método
            if (endereco.getEnderecoCompleto() != null && !endereco.getEnderecoCompleto().isEmpty()) {
                Endereco resultado = geocodingService.geocodificar(endereco.getEnderecoCompleto());
                return ResponseEntity.ok(resultado);
            } else {
                // Se veio com campos separados, usa o método com objeto
                Endereco resultado = geocodingService.geocodificar(endereco);
                return ResponseEntity.ok(resultado);
            }
            
        } catch (Exception e) {
            logger.error("Erro no controller ao geocodificar: {}", endereco, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Endereco>> buscarEnderecos(@RequestParam String query) {
        try {
            logger.info("Buscando endereços para: {}", query);
            List<Endereco> resultados = geocodingService.buscarEnderecos(query);
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            logger.error("Erro ao buscar endereços: {}", query, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/reverse")
    public ResponseEntity<Endereco> reverseGeocoding(
            @RequestParam Double lat, 
            @RequestParam Double lon) {
        try {
            logger.info("Reverse geocoding para: {}, {}", lat, lon);
            Endereco resultado = geocodingService.reverseGeocoding(lat, lon);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            logger.error("Erro no reverse geocoding: {}, {}", lat, lon, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Geocoding Service OK");
    }
}