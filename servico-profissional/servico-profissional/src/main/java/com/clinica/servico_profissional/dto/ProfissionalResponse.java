package com.clinica.servico_profissional.dto;

import com.clinica.servico_profissional.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalResponse {

    private Long id;
    private String nome;
    private String email;
    private String cro;
    private String especialidade;
    private Role role;
    private boolean ativo;
}
