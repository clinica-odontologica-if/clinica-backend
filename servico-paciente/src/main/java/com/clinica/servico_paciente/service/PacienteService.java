package com.clinica.servico_paciente.service;

import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public List<PacienteResponse> listarAtivos() {
        return pacienteRepository.findByAtivoTrue()
                .stream()
                .map(PacienteResponse::from)
                .toList();
    }
}
