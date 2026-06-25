package com.clinica.servico_profissional.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErroResponse {

    private int status;
    private String erro;
    private String mensagem;
    private String caminho;
}
