package com.clinica.servico_financeiro.dto;

import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceitaRequest(
        @NotNull(message = "Atendimento e obrigatorio")
        Long atendimentoId,

        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String descricao,

        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Forma de pagamento e obrigatoria")
        FormaPagamento formaPagamento,

        StatusFinanceiro status,
        LocalDate dataVencimento,
        LocalDate dataPagamento
) {
}