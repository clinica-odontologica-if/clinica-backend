package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.exception.RecursoNaoEncontradoException;
import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;

    public List<ProfissionalResponse> listarAtivos() {
        return profissionalRepository.findByAtivoTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProfissionalResponse buscarPorId(Long id) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " não encontrado"
                ));
        return toResponse(profissional);
    }

    private ProfissionalResponse toResponse(Profissional profissional) {
        return new ProfissionalResponse(
                profissional.getId(),
                profissional.getNome(),
                profissional.getEmail(),
                profissional.getCro(),
                profissional.getEspecialidade(),
                profissional.getRole(),
                profissional.isAtivo()
        );
    }
}
