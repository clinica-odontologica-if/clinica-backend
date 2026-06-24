package com.clinica.servico_paciente.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private LocalDate nascimento;
    private String cpf;
    private String endereco;
    private String telefone;
    private String email;
    private String observacoes;

    private boolean ativo = true;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
