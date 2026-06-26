package com.clinica.servico_paciente.dto;

import com.clinica.servico_paciente.model.Paciente;

import java.time.LocalDateTime;

public record PacienteResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        boolean ativo,
        LocalDateTime criadoEm
) {
    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getEmail(),
                paciente.getTelefone(),
                paciente.isAtivo(),
                paciente.getCriadoEm()
        );
    }
}
