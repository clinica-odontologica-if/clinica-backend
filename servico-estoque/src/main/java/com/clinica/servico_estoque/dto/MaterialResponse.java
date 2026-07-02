package com.clinica.servico_estoque.dto;

import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        String nome,
        String descricao,
        String categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        boolean baixoEstoque,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getNome(),
                material.getDescricao(),
                material.getCategoria(),
                material.getUnidadeMedida(),
                material.getQuantidadeAtual(),
                material.getQuantidadeMinima(),
                material.isBaixoEstoque(),
                material.isAtivo(),
                material.getCriadoEm(),
                material.getAtualizadoEm()
        );
    }
}
