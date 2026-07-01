package com.clinica.servico_atendimento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RealizacaoAtendimentoRequest(
        @Size(max = 255, message = "Procedimento realizado deve ter no maximo 255 caracteres")
        String procedimentoRealizado,

        @Size(max = 500, message = "Observacoes devem ter no maximo 500 caracteres")
        String observacoes,

        @DecimalMin(value = "0.00", message = "Valor nao pode ser negativo")
        BigDecimal valor
) {
}
