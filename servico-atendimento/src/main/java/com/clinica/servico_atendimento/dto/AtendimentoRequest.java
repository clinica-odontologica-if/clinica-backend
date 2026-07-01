package com.clinica.servico_atendimento.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AtendimentoRequest(
        @NotNull(message = "Paciente e obrigatorio")
        Long pacienteId,

        @NotNull(message = "Profissional e obrigatorio")
        Long profissionalId,

        @NotNull(message = "Data do atendimento e obrigatoria")
        @FutureOrPresent(message = "Data do atendimento nao pode ser passada")
        LocalDate data,

        @NotNull(message = "Hora do atendimento e obrigatoria")
        LocalTime hora,

        @Min(value = 15, message = "Duracao minima do atendimento e de 15 minutos")
        @Max(value = 480, message = "Duracao maxima do atendimento e de 480 minutos")
        Integer duracaoMinutos,

        @Size(max = 500, message = "Observacoes devem ter no maximo 500 caracteres")
        String observacoes
) {
}
