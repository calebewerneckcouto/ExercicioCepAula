package com.cwcdev.ia.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.repository.EnderecoRepository;

@Service
@Transactional
public class EnderecoServiceImpl implements EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;
    
    @Autowired
    private GeocodingService geocodingService;

    @Override
    public Endereco salvarEndereco(Endereco endereco) {
        try {
            // Verificar se o endereço já possui coordenadas
            if (endereco.getLatitude() == null || endereco.getLongitude() == null) {
                // Realizar geocodificação se não tiver coordenadas
                endereco = geocodificarEndereco(endereco);
            }
            
            // Verificar se endereço similar já existe
            Optional<Endereco> enderecoExistente = buscarEnderecoSimilar(endereco);
            if (enderecoExistente.isPresent()) {
                return enderecoExistente.get();
            }
            
            // Salvar no banco de dados
            return enderecoRepository.save(endereco);
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar endereço: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Endereco> buscarEnderecoPorId(Long id) {
        return enderecoRepository.findById(id);
    }

    @Override
    public List<Endereco> buscarTodosEnderecos() {
        return enderecoRepository.findAll();
    }

    @Override
    public List<Endereco> buscarEnderecosPorCidade(String cidade) {
        return enderecoRepository.findByCidadeContainingIgnoreCase(cidade);
    }

    @Override
    public List<Endereco> buscarEnderecosPorEstado(String estado) {
        return enderecoRepository.findByEstadoContainingIgnoreCase(estado);
    }

    @Override
    public Endereco atualizarEndereco(Long id, Endereco enderecoAtualizado) {
        Optional<Endereco> enderecoOptional = enderecoRepository.findById(id);
        
        if (enderecoOptional.isPresent()) {
            Endereco enderecoExistente = enderecoOptional.get();
            
            // Atualizar campos se fornecidos
            if (enderecoAtualizado.getRua() != null) {
                enderecoExistente.setRua(enderecoAtualizado.getRua());
            }
            if (enderecoAtualizado.getNumero() != null) {
                enderecoExistente.setNumero(enderecoAtualizado.getNumero());
            }
            if (enderecoAtualizado.getBairro() != null) {
                enderecoExistente.setBairro(enderecoAtualizado.getBairro());
            }
            if (enderecoAtualizado.getCidade() != null) {
                enderecoExistente.setCidade(enderecoAtualizado.getCidade());
            }
            if (enderecoAtualizado.getEstado() != null) {
                enderecoExistente.setEstado(enderecoAtualizado.getEstado());
            }
            
            // Se algum campo de endereço foi alterado, re-geocodificar
            boolean enderecoModificado = enderecoAtualizado.getRua() != null ||
                                       enderecoAtualizado.getNumero() != null ||
                                       enderecoAtualizado.getBairro() != null ||
                                       enderecoAtualizado.getCidade() != null ||
                                       enderecoAtualizado.getEstado() != null;
            
            if (enderecoModificado) {
                enderecoExistente.setLatitude(null);
                enderecoExistente.setLongitude(null);
                return geocodificarEndereco(enderecoExistente);
            }
            
            return enderecoRepository.save(enderecoExistente);
        } else {
            throw new RuntimeException("Endereço não encontrado com ID: " + id);
        }
    }

    @Override
    public void excluirEndereco(Long id) {
        if (enderecoRepository.existsById(id)) {
            enderecoRepository.deleteById(id);
        } else {
            throw new RuntimeException("Endereço não encontrado com ID: " + id);
        }
    }

    @Override
    public Endereco geocodificarEndereco(Endereco endereco) {
        try {
            Endereco enderecoGeocodificado = geocodingService.geocodificar(endereco);
            
            // Atualizar coordenadas no endereço original
            endereco.setLatitude(enderecoGeocodificado.getLatitude());
            endereco.setLongitude(enderecoGeocodificado.getLongitude());
            
            return endereco;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha na geocodificação do endereço: " + e.getMessage(), e);
        }
    }

    // NOVOS MÉTODOS ADICIONADOS PARA JAVA 8
    @Override
    public Endereco geocodificar(String enderecoCompleto) {
        try {
            Endereco endereco = new Endereco();
            endereco.setRua(enderecoCompleto); // Temporário, será processado pelo GeocodingService
            
            Endereco enderecoGeocodificado = geocodingService.geocodificar(enderecoCompleto);
            
            // Atualizar com dados retornados
            endereco.setRua(enderecoGeocodificado.getRua());
            endereco.setNumero(enderecoGeocodificado.getNumero());
            endereco.setBairro(enderecoGeocodificado.getBairro());
            endereco.setCidade(enderecoGeocodificado.getCidade());
            endereco.setEstado(enderecoGeocodificado.getEstado());
            endereco.setLatitude(enderecoGeocodificado.getLatitude());
            endereco.setLongitude(enderecoGeocodificado.getLongitude());
            
            return endereco;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha na geocodificação do endereço: " + enderecoCompleto, e);
        }
    }

    @Override
    public List<Endereco> buscarEnderecos(String query) {
        try {
            return geocodingService.buscarEnderecos(query);
        } catch (Exception e) {
            throw new RuntimeException("Falha na busca de endereços: " + query, e);
        }
    }

    @Override
    public Endereco reverseGeocoding(Double latitude, Double longitude) {
        try {
            return geocodingService.reverseGeocoding(latitude, longitude);
        } catch (Exception e) {
            throw new RuntimeException("Falha no reverse geocoding para: " + latitude + ", " + longitude, e);
        }
    }

    @Override
    public Optional<Endereco> buscarEnderecoPorCoordenadas(Double latitude, Double longitude) {
        // Buscar por coordenadas aproximadas (com tolerância de 0.001 graus ~ 111m)
        double tolerancia = 0.001;
        List<Endereco> enderecos = enderecoRepository.findByLatitudeBetweenAndLongitudeBetween(
            latitude - tolerancia, 
            latitude + tolerancia,
            longitude - tolerancia, 
            longitude + tolerancia
        );
        
        if (enderecos != null && !enderecos.isEmpty()) {
            return Optional.of(enderecos.get(0));
        }
        
        return Optional.empty();
    }

    @Override
    public boolean enderecoExiste(Endereco endereco) {
        return buscarEnderecoSimilar(endereco).isPresent();
    }

    @Override
    public String formatarEnderecoCompleto(Endereco endereco) {
        StringBuilder enderecoCompleto = new StringBuilder();
        
        if (endereco.getRua() != null) {
            enderecoCompleto.append(endereco.getRua());
            if (endereco.getNumero() != null) {
                enderecoCompleto.append(", ").append(endereco.getNumero());
            }
        }
        
        if (endereco.getBairro() != null) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append(" - ");
            enderecoCompleto.append(endereco.getBairro());
        }
        
        if (endereco.getCidade() != null) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append(", ");
            enderecoCompleto.append(endereco.getCidade());
        }
        
        if (endereco.getEstado() != null) {
            if (enderecoCompleto.length() > 0) enderecoCompleto.append("/");
            enderecoCompleto.append(endereco.getEstado());
        }
        
        return enderecoCompleto.toString();
    }

    /**
     * Busca endereço similar no banco para evitar duplicatas
     */
    private Optional<Endereco> buscarEnderecoSimilar(Endereco endereco) {
        // Buscar por endereço completo
        if (endereco.getRua() != null && endereco.getNumero() != null && 
            endereco.getCidade() != null && endereco.getEstado() != null) {
            
            Optional<Endereco> enderecoExistente = enderecoRepository.findByRuaAndNumeroAndCidadeAndEstado(
                endereco.getRua(), 
                endereco.getNumero(), 
                endereco.getCidade(), 
                endereco.getEstado()
            );
            
            return enderecoExistente;
        }
        
        // Buscar por coordenadas se disponíveis
        if (endereco.getLatitude() != null && endereco.getLongitude() != null) {
            return buscarEnderecoPorCoordenadas(endereco.getLatitude(), endereco.getLongitude());
        }
        
        return Optional.empty();
    }
}