package com.clinica.servico_estoque.dto;

import com.clinica.servico_estoque.model.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MaterialRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter no maximo 120 caracteres")
        String nome,

        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String descricao,

        @Size(max = 80, message = "Categoria deve ter no maximo 80 caracteres")
        String categoria,

        @NotNull(message = "Unidade de medida e obrigatoria")
        UnidadeMedida unidadeMedida,

        @DecimalMin(value = "0.00", message = "Quantidade atual nao pode ser negativa")
        BigDecimal quantidadeAtual,

        @NotNull(message = "Quantidade minima e obrigatoria")
        @DecimalMin(value = "0.00", message = "Quantidade minima nao pode ser negativa")
        BigDecimal quantidadeMinima
) {
}
