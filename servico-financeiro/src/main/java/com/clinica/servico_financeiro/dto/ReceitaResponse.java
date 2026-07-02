package com.clinica.servico_financeiro.dto;

import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.model.StatusFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceitaResponse(
        Long id,
        Long atendimentoId,
        Long pacienteId,
        Long profissionalId,
        String descricao,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        StatusFinanceiro status,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static ReceitaResponse from(Receita receita) {
        return new ReceitaResponse(
                receita.getId(),
                receita.getAtendimentoId(),
                receita.getPacienteId(),
                receita.getProfissionalId(),
                receita.getDescricao(),
                receita.getValor(),
                receita.getFormaPagamento(),
                receita.getStatus(),
                receita.getDataVencimento(),
                receita.getDataPagamento(),
                receita.isAtivo(),
                receita.getCriadoEm(),
                receita.getAtualizadoEm()
        );
    }
}