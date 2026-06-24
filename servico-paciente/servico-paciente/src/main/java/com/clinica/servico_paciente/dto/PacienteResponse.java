package com.clinica.servico_paciente.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResponse {
    private Long id;
    private String nome;
    private LocalDate nascimento;
    private String cpf;
    private String endereco;
    private String telefone;
    private String email;
    private String observacoes;
    private boolean ativo;
}