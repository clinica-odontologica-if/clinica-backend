package com.clinica.servico_atendimento.model;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "atendimentos")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "paciente_nome", nullable = false, length = 120)
    private String pacienteNome;

    @Column(name = "profissional_id", nullable = false)
    private Long profissionalId;

    @Column(name = "profissional_nome", nullable = false, length = 120)
    private String profissionalNome;

    @Column(name = "profissional_email", nullable = false, length = 150)
    private String profissionalEmail;

    @Column(name = "data_atendimento", nullable = false)
    private LocalDate dataAtendimento;

    @Column(name = "hora_atendimento", nullable = false)
    private LocalTime horaAtendimento;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtendimento status = StatusAtendimento.AGENDADO;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "procedimento_realizado", length = 255)
    private String procedimentoRealizado;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "realizado_em")
    private LocalDateTime realizadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (duracaoMinutos == null) {
            duracaoMinutos = 60;
        }
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
