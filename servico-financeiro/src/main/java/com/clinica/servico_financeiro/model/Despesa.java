package com.clinica.servico_financeiro.model;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "despesas")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private CategoriaDespesa categoria;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusFinanceiro status = StatusFinanceiro.PENDENTE;

    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private boolean ativo = true;
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusFinanceiro.PENDENTE;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}