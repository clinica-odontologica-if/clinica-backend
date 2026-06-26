package com.clinica.servico_profissional.dto;

import com.clinica.servico_profissional.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioInternoRequest {

    private String nome;
    private String email;
    private String senha;
    private Role role;
}
