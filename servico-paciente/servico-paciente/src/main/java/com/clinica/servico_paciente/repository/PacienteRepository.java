package com.clinica.servico_paciente.repository;

import com.clinica.servico_paciente.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}