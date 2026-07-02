package com.clinica.servico_estoque.dto;

import com.clinica.servico_estoque.model.MovimentacaoEstoque;
import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoEstoqueResponse(
        Long id,
        Long materialId,
        String materialNome,
        TipoMovimentacaoEstoque tipo,
        BigDecimal quantidade,
        BigDecimal saldoAnterior,
        BigDecimal saldoAtual,
        String motivo,
        String usuarioEmail,
        LocalDateTime criadoEm
) {

    public static MovimentacaoEstoqueResponse from(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoEstoqueResponse(
                movimentacao.getId(),
                movimentacao.getMaterial().getId(),
                movimentacao.getMaterial().getNome(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getSaldoAnterior(),
                movimentacao.getSaldoAtual(),
                movimentacao.getMotivo(),
                movimentacao.getUsuarioEmail(),
                movimentacao.getCriadoEm()
        );
    }
}
