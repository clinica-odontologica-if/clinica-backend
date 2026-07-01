package com.clinica.servico_financeiro.client;

import java.math.BigDecimal;

public record AtendimentoClientResponse(
        Long id,
        Long pacienteId,
        Long profissionalId,
        String status,
        BigDecimal valor,
        boolean ativo
) {
}