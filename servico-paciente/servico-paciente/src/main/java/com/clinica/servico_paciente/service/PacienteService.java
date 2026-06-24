package com.clinica.servico_paciente.service;

import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.model.Paciente;
import com.clinica.servico_paciente.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public List<PacienteResponse> listarAtivos() {
        return pacienteRepository.findAll()
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public PacienteResponse buscarPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente nao encontrado"));
        return converterParaResponse(paciente);
    }

    private PacienteResponse converterParaResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getNascimento(),
                paciente.getCpf(),
                paciente.getEndereco(),
                paciente.getTelefone(),
                paciente.getEmail(),
                paciente.getObservacoes(),
                paciente.isAtivo()
        );
    }
}