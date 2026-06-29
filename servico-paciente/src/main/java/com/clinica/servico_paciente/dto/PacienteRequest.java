package com.clinica.servico_paciente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "CPF e obrigatorio")
        @Size(max = 14, message = "CPF deve ter no maximo 14 caracteres")
        String cpf,

        @NotNull(message = "Data de nascimento e obrigatoria")
        @PastOrPresent(message = "Data de nascimento nao pode ser futura")
        LocalDate dataNascimento,

        @NotBlank(message = "Telefone e obrigatorio")
        @Size(max = 20, message = "Telefone deve ter no maximo 20 caracteres")
        String telefone,

        @Email(message = "Email invalido")
        @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
        String email,

        @Size(max = 255, message = "Endereco deve ter no maximo 255 caracteres")
        String endereco,

        @Size(max = 500, message = "Observacoes devem ter no maximo 500 caracteres")
        String observacoes
) {
}
