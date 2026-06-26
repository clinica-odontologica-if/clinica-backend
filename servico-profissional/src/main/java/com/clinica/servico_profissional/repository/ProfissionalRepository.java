package com.clinica.servico_profissional.repository;

import com.clinica.servico_profissional.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByAtivoTrue();

    Optional<Profissional> findByEmail(String email);

    boolean existsByEmail(String email);
}
