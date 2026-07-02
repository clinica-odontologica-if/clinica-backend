package com.clinica.servico_financeiro.dto;

import com.clinica.servico_financeiro.model.StatusFinanceiro;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StatusFinanceiroRequest(
        @NotNull(message = "Status e obrigatorio")
        StatusFinanceiro status,
        LocalDate dataPagamento
) {
}