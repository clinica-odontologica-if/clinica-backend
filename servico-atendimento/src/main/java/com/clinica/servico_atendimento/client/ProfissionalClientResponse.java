package com.clinica.servico_atendimento.client;

public record ProfissionalClientResponse(
        Long id,
        String nome,
        String email,
        String cro,
        String especialidade,
        String role,
        boolean ativo
) {
}
