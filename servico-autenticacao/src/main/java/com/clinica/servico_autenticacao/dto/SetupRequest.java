package com.clinica.servico_autenticacao.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetupRequest {

    private String nome;
    private String email;
    private String senha;
}