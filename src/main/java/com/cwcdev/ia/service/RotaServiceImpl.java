package com.cwcdev.ia.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.model.Rota;
import com.cwcdev.ia.repository.RotaRepository;

@Service
@Transactional
public class RotaServiceImpl implements RotaService {

    @Autowired
    private RotaRepository rotaRepository;
    
    @Autowired
    private EnderecoService enderecoService;
    
    @Autowired
    private RoutingService routingService;

    @Override
    public Rota calcularRota(Endereco origem, Endereco destino) {
        try {
            // Validar endereços
            if (origem == null || destino == null) {
                throw new IllegalArgumentException("Endereço de origem e destino são obrigatórios");
            }
            
            // Verificar se endereços possuem coordenadas
            if (origem.getLatitude() == null || origem.getLongitude() == null) {
                origem = enderecoService.geocodificarEndereco(origem);
            }
            
            if (destino.getLatitude() == null || destino.getLongitude() == null) {
                destino = enderecoService.geocodificarEndereco(destino);
            }
            
            // Verificar se rota já existe
            Optional<Rota> rotaExistente = rotaRepository.findByOrigemAndDestino(origem, destino);
            if (rotaExistente.isPresent()) {
                return rotaExistente.get();
            }
            
            // Calcular rota usando serviço de roteamento
            Rota rotaCalculada = routingService.calcularRota(origem, destino);
            
            // Salvar no banco
            return salvarRota(rotaCalculada);
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular rota: " + e.getMessage(), e);
        }
    }
    
    
    
    @Override
    public Optional<Rota> buscarRotaPorId(Long id) {
        return rotaRepository.findById(id);
    }
    
    @Override
    public Rota buscarRotaPorIdOuFalhar(Long id) {
        return rotaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rota não encontrada com ID: " + id));
    }

    @Override
    public Rota calcularRota(Long origemId, Long destinoId) {
        Endereco origem = enderecoService.buscarEnderecoPorId(origemId)
            .orElseThrow(() -> new RuntimeException("Endereço de origem não encontrado: " + origemId));
        
        Endereco destino = enderecoService.buscarEnderecoPorId(destinoId)
            .orElseThrow(() -> new RuntimeException("Endereço de destino não encontrado: " + destinoId));
        
        return calcularRota(origem, destino);
    }

    @Override
    public Rota salvarRota(Rota rota) {
        try {
            // Validar rota antes de salvar
            validarRota(rota);
            
            // Verificar duplicatas
            Optional<Rota> rotaExistente = rotaRepository.findByOrigemAndDestino(
                rota.getOrigem(), rota.getDestino());
            
            if (rotaExistente.isPresent()) {
                // Atualizar rota existente
                Rota existente = rotaExistente.get();
                existente.setDistanciaKm(rota.getDistanciaKm());
                existente.setDuracaoMinutos(rota.getDuracaoMinutos());
                existente.setPolyline(rota.getPolyline());
                return rotaRepository.save(existente);
            }
            
            return rotaRepository.save(rota);
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar rota: " + e.getMessage(), e);
        }
    }

   

    @Override
    public List<Rota> buscarTodasRotas() {
        return rotaRepository.findAllByOrderByDataCriacaoDesc();
    }

    @Override
    public List<Rota> buscarRotasPorOrigem(Long enderecoOrigemId) {
        return enderecoService.buscarEnderecoPorId(enderecoOrigemId)
            .map(rotaRepository::findByOrigem)
            .orElseThrow(() -> new RuntimeException("Endereço de origem não encontrado: " + enderecoOrigemId));
    }

    @Override
    public List<Rota> buscarRotasPorDestino(Long enderecoDestinoId) {
        return enderecoService.buscarEnderecoPorId(enderecoDestinoId)
            .map(rotaRepository::findByDestino)
            .orElseThrow(() -> new RuntimeException("Endereço de destino não encontrado: " + enderecoDestinoId));
    }

    @Override
    public Rota atualizarRota(Long id, Rota rotaAtualizada) {
        return rotaRepository.findById(id)
            .map(rotaExistente -> {
                // Atualizar campos permitidos
                if (rotaAtualizada.getDistanciaKm() != null) {
                    rotaExistente.setDistanciaKm(rotaAtualizada.getDistanciaKm());
                }
                if (rotaAtualizada.getDuracaoMinutos() != null) {
                    rotaExistente.setDuracaoMinutos(rotaAtualizada.getDuracaoMinutos());
                }
                if (rotaAtualizada.getPolyline() != null) {
                    rotaExistente.setPolyline(rotaAtualizada.getPolyline());
                }
                
                return rotaRepository.save(rotaExistente);
            })
            .orElseThrow(() -> new RuntimeException("Rota não encontrada com ID: " + id));
    }

    @Override
    public void excluirRota(Long id) {
        if (rotaRepository.existsById(id)) {
            rotaRepository.deleteById(id);
        } else {
            throw new RuntimeException("Rota não encontrada com ID: " + id);
        }
    }

    @Override
    public boolean rotaExiste(Long origemId, Long destinoId) {
        Endereco origem = enderecoService.buscarEnderecoPorId(origemId).orElse(null);
        Endereco destino = enderecoService.buscarEnderecoPorId(destinoId).orElse(null);
        
        if (origem == null || destino == null) {
            return false;
        }
        
        return rotaRepository.findByOrigemAndDestino(origem, destino).isPresent();
    }

    @Override
    public Optional<Rota> buscarRotaPorOrigemEDestino(Long origemId, Long destinoId) {
        Endereco origem = enderecoService.buscarEnderecoPorId(origemId).orElse(null);
        Endereco destino = enderecoService.buscarEnderecoPorId(destinoId).orElse(null);
        
        if (origem == null || destino == null) {
            return Optional.empty();
        }
        
        return rotaRepository.findByOrigemAndDestino(origem, destino);
    }
    
    /**
     * Valida se a rota está correta antes de salvar
     */
    private void validarRota(Rota rota) {
        if (rota.getOrigem() == null) {
            throw new IllegalArgumentException("Endereço de origem é obrigatório");
        }
        if (rota.getDestino() == null) {
            throw new IllegalArgumentException("Endereço de destino é obrigatório");
        }
        if (rota.getOrigem().equals(rota.getDestino())) {
            throw new IllegalArgumentException("Endereço de origem e destino não podem ser iguais");
        }
        if (rota.getDistanciaKm() != null && rota.getDistanciaKm() < 0) {
            throw new IllegalArgumentException("Distância não pode ser negativa");
        }
        if (rota.getDuracaoMinutos() != null && rota.getDuracaoMinutos() < 0) {
            throw new IllegalArgumentException("Duração não pode ser negativa");
        }
    }
}