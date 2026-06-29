package com.clinica.servico_paciente.dto;

import com.clinica.servico_paciente.model.Paciente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String email,
        String telefone,
        String endereco,
        String observacoes,
        boolean ativo,
        LocalDateTime criadoEm
) {
    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getDataNascimento(),
                paciente.getEmail(),
                paciente.getTelefone(),
                paciente.getEndereco(),
                paciente.getObservacoes(),
                paciente.isAtivo(),
                paciente.getCriadoEm()
        );
    }
}
