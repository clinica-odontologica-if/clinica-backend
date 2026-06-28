package com.clinica.servico_paciente.repository;

import com.clinica.servico_paciente.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByAtivoTrue();

    @Query("""
            SELECT p
            FROM Paciente p
            WHERE p.ativo = true
              AND (:busca IS NULL OR
                   LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(p.email) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   p.cpf LIKE CONCAT('%', :busca, '%') OR
                   p.telefone LIKE CONCAT('%', :busca, '%'))
              AND (:cpf IS NULL OR p.cpf LIKE CONCAT('%', :cpf, '%'))
            """)
    List<Paciente> buscarAtivosComFiltros(@Param("busca") String busca,
                                           @Param("cpf") String cpf);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Paciente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}
