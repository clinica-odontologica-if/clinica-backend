package com.clinica.servico_profissional.repository;

import com.clinica.servico_profissional.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByAtivoTrue();
}
