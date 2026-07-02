package com.clinica.servico_estoque.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "materiais")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(length = 80)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 20)
    private UnidadeMedida unidadeMedida;

    @Column(name = "quantidade_atual", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidadeAtual = BigDecimal.ZERO;

    @Column(name = "quantidade_minima", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidadeMinima = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (quantidadeAtual == null) {
            quantidadeAtual = BigDecimal.ZERO;
        }
        if (quantidadeMinima == null) {
            quantidadeMinima = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean isBaixoEstoque() {
        return quantidadeAtual.compareTo(quantidadeMinima) <= 0;
    }
}
