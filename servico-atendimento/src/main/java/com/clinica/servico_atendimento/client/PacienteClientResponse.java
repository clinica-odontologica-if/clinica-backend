package com.clinica.servico_atendimento.client;

public record PacienteClientResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        boolean ativo
) {
}
