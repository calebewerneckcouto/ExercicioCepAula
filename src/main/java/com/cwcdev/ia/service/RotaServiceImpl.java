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
            
            // Persistir endereços ANTES de usar na rota
            // Isso garante que ambos terão IDs
            Endereco origemSalva = enderecoService.salvarEndereco(origem);
            Endereco destinoSalvo = enderecoService.salvarEndereco(destino);
            
            // Verificar se endereços possuem coordenadas
            if (origemSalva.getLatitude() == null || origemSalva.getLongitude() == null) {
                origemSalva = enderecoService.geocodificarEndereco(origemSalva);
            }
            
            if (destinoSalvo.getLatitude() == null || destinoSalvo.getLongitude() == null) {
                destinoSalvo = enderecoService.geocodificarEndereco(destinoSalvo);
            }
            
            // Verificar se rota já existe
            Optional<Rota> rotaExistente = rotaRepository.findByOrigemAndDestino(origemSalva, destinoSalvo);
            if (rotaExistente.isPresent()) {
                return rotaExistente.get();
            }
            
            // Calcular rota usando serviço de roteamento
            Rota rotaCalculada = routingService.calcularRota(origemSalva, destinoSalvo);
            
            // Garantir que a rota use os endereços persistidos
            rotaCalculada.setOrigem(origemSalva);
            rotaCalculada.setDestino(destinoSalvo);
            
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
            
            // Garantir que os endereços estão persistidos
            if (rota.getOrigem().getId() == null) {
                Endereco origemSalva = enderecoService.salvarEndereco(rota.getOrigem());
                rota.setOrigem(origemSalva);
            }
            
            if (rota.getDestino().getId() == null) {
                Endereco destinoSalvo = enderecoService.salvarEndereco(rota.getDestino());
                rota.setDestino(destinoSalvo);
            }
            
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
        if (rota == null) {
            throw new IllegalArgumentException("Rota é obrigatória");
        }
        if (rota.getOrigem() == null) {
            throw new IllegalArgumentException("Endereço de origem é obrigatório");
        }
        if (rota.getDestino() == null) {
            throw new IllegalArgumentException("Endereço de destino é obrigatório");
        }
        // Removida validação que exigia ID - agora validamos apenas a existência do objeto
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