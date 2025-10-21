package com.cwcdev.ia.controller;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.service.EnderecoService;
import com.cwcdev.ia.service.RotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rotas")
public class RotaController {
    
    @Autowired
    private RotaService rotaService;
    
    @Autowired
    private EnderecoService enderecoService;
    
    @PostMapping("/calcular")
    public ResponseEntity<?> calcularRota(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("Recebendo requisição de rota: " + request);
            
            // Extrai origem e destino
            Map<String, Object> origemMap = (Map<String, Object>) request.get("origem");
            Map<String, Object> destinoMap = (Map<String, Object>) request.get("destino");
            
            if (origemMap == null || destinoMap == null) {
                return ResponseEntity.badRequest().body("Origem e destino são obrigatórios");
            }
            
            // Cria e SALVA endereço de origem primeiro
            Endereco origem = new Endereco();
            origem.setRua(getStringValue(origemMap, "rua"));
            origem.setNumero(getStringValue(origemMap, "numero"));
            origem.setBairro(getStringValue(origemMap, "bairro"));
            origem.setCidade(getStringValue(origemMap, "cidade"));
            origem.setEstado(getStringValue(origemMap, "estado"));
            
            // Se tem coordenadas, usa elas
            if (origemMap.get("latitude") != null && origemMap.get("longitude") != null) {
                origem.setLatitude(Double.parseDouble(origemMap.get("latitude").toString()));
                origem.setLongitude(Double.parseDouble(origemMap.get("longitude").toString()));
            }
            
            // SALVA a origem primeiro
            origem = enderecoService.salvarEndereco(origem);
            
            // Cria e SALVA endereço de destino
            Endereco destino = new Endereco();
            destino.setRua(getStringValue(destinoMap, "rua"));
            destino.setNumero(getStringValue(destinoMap, "numero"));
            destino.setBairro(getStringValue(destinoMap, "bairro"));
            destino.setCidade(getStringValue(destinoMap, "cidade"));
            destino.setEstado(getStringValue(destinoMap, "estado"));
            
            // Se tem coordenadas, usa elas
            if (destinoMap.get("latitude") != null && destinoMap.get("longitude") != null) {
                destino.setLatitude(Double.parseDouble(destinoMap.get("latitude").toString()));
                destino.setLongitude(Double.parseDouble(destinoMap.get("longitude").toString()));
            }
            
            // SALVA o destino primeiro
            destino = enderecoService.salvarEndereco(destino);
            
            System.out.println("Origem salva: " + origem);
            System.out.println("Destino salvo: " + destino);
            
            // Verifica se temos coordenadas
            if (origem.getLatitude() == null || destino.getLatitude() == null) {
                return ResponseEntity.badRequest().body("Não foi possível obter coordenadas para origem ou destino");
            }
            
            // Calcula rota
            Rota rota = rotaService.calcularRota(origem, destino);
            Rota rotaSalva = rotaService.salvarRota(rota);
            
            Map<String, Object> response = new HashMap<>();
            response.put("rota", rotaSalva);
            response.put("origem", origem);
            response.put("destino", destino);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Erro ao calcular rota: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Erro ao calcular rota: " + e.getMessage());
        }
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    @GetMapping
    public ResponseEntity<?> listarRotas() {
        try {
            return ResponseEntity.ok(rotaService.buscarTodasRotas());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao listar rotas: " + e.getMessage());
        }
    }
}