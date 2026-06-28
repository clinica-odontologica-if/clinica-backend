package com.clinica.servico_profissional.repository;

import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByAtivoTrue();

    @Query("""
            SELECT p
            FROM Profissional p
            WHERE p.ativo = true
              AND (:busca IS NULL OR
                   LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(p.email) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(p.cro) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(p.especialidade) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:especialidade IS NULL OR LOWER(p.especialidade) LIKE LOWER(CONCAT('%', :especialidade, '%')))
              AND (:role IS NULL OR p.role = :role)
            """)
    List<Profissional> buscarAtivosComFiltros(@Param("busca") String busca,
                                               @Param("especialidade") String especialidade,
                                               @Param("role") Role role);

    Optional<Profissional> findByEmail(String email);

    boolean existsByEmail(String email);
}
