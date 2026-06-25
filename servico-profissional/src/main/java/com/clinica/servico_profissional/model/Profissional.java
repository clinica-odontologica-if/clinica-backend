package com.clinica.servico_profissional.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profissionais")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String cro;
    private String especialidade;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean ativo = true;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
