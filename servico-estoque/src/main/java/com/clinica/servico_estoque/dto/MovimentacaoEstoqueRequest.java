package com.clinica.servico_estoque.dto;

import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MovimentacaoEstoqueRequest(
        @NotNull(message = "Tipo de movimentacao e obrigatorio")
        TipoMovimentacaoEstoque tipo,

        @NotNull(message = "Quantidade e obrigatoria")
        @DecimalMin(value = "0.00", message = "Quantidade nao pode ser negativa")
        BigDecimal quantidade,

        @Size(max = 500, message = "Motivo deve ter no maximo 500 caracteres")
        String motivo
) {
}
