package com.clinica.servico_paciente.dto;

import java.time.LocalDateTime;

public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        LocalDateTime timestamp
) {
    public ErroResponse(int status, String erro, String mensagem, String caminho) {
        this(status, erro, mensagem, caminho, LocalDateTime.now());
    }
}
