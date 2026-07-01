package com.clinica.servico_financeiro.dto;

import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.StatusFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DespesaResponse(
        Long id,
        String descricao,
        CategoriaDespesa categoria,
        BigDecimal valor,
        StatusFinanceiro status,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static DespesaResponse from(Despesa despesa) {
        return new DespesaResponse(
                despesa.getId(),
                despesa.getDescricao(),
                despesa.getCategoria(),
                despesa.getValor(),
                despesa.getStatus(),
                despesa.getDataVencimento(),
                despesa.getDataPagamento(),
                despesa.isAtivo(),
                despesa.getCriadoEm(),
                despesa.getAtualizadoEm()
        );
    }
}