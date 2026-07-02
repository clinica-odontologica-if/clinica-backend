package com.clinica.servico_financeiro.dto;

import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RelatorioFinanceiroResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        Map<FormaPagamento, BigDecimal> receitasPorFormaPagamento,
        Map<CategoriaDespesa, BigDecimal> despesasPorCategoria
) {
}