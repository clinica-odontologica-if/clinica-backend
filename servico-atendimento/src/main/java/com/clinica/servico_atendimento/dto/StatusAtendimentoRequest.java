package com.clinica.servico_atendimento.dto;

import com.clinica.servico_atendimento.model.StatusAtendimento;
import jakarta.validation.constraints.NotNull;

public record StatusAtendimentoRequest(
        @NotNull(message = "Status e obrigatorio")
        StatusAtendimento status
) {
}
