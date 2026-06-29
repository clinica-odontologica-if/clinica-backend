package com.clinica.servico_atendimento.dto;

import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AtendimentoResponse(
        Long id,
        Long pacienteId,
        String pacienteNome,
        Long profissionalId,
        String profissionalNome,
        String profissionalEmail,
        LocalDate data,
        LocalTime hora,
        StatusAtendimento status,
        String observacoes,
        String procedimentoRealizado,
        BigDecimal valor,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime realizadoEm
) {
    public static AtendimentoResponse from(Atendimento atendimento) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getPacienteId(),
                atendimento.getPacienteNome(),
                atendimento.getProfissionalId(),
                atendimento.getProfissionalNome(),
                atendimento.getProfissionalEmail(),
                atendimento.getDataAtendimento(),
                atendimento.getHoraAtendimento(),
                atendimento.getStatus(),
                atendimento.getObservacoes(),
                atendimento.getProcedimentoRealizado(),
                atendimento.getValor(),
                atendimento.isAtivo(),
                atendimento.getCriadoEm(),
                atendimento.getAtualizadoEm(),
                atendimento.getRealizadoEm()
        );
    }
}
