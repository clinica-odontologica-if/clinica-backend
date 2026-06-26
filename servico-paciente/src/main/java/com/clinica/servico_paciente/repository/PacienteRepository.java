package com.clinica.servico_paciente.repository;

import com.clinica.servico_paciente.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByAtivoTrue();

    boolean existsByEmail(String email);
}
