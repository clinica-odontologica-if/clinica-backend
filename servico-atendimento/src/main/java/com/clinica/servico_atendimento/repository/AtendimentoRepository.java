package com.clinica.servico_atendimento.repository;

import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Optional<Atendimento> findByIdAndAtivoTrue(Long id);

    @Query("""
            SELECT a
            FROM Atendimento a
            WHERE a.ativo = true
              AND a.dataAtendimento = :data
              AND a.status IN :status
              AND (a.pacienteId = :pacienteId OR a.profissionalId = :profissionalId)
            ORDER BY a.horaAtendimento ASC
            """)
    List<Atendimento> buscarAtendimentosQueOcupamAgenda(@Param("pacienteId") Long pacienteId,
                                                         @Param("profissionalId") Long profissionalId,
                                                         @Param("data") LocalDate data,
                                                         @Param("status") Collection<StatusAtendimento> status);

    @Query("""
            SELECT a
            FROM Atendimento a
            WHERE a.ativo = true
              AND (:pacienteId IS NULL OR a.pacienteId = :pacienteId)
              AND (:profissionalId IS NULL OR a.profissionalId = :profissionalId)
              AND (:data IS NULL OR a.dataAtendimento = :data)
              AND (:dataInicio IS NULL OR a.dataAtendimento >= :dataInicio)
              AND (:dataFim IS NULL OR a.dataAtendimento <= :dataFim)
              AND (:status IS NULL OR a.status = :status)
              AND (:busca IS NULL OR LOWER(a.pacienteNome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.profissionalNome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.profissionalEmail) LIKE LOWER(CONCAT('%', :busca, '%')))
            ORDER BY a.dataAtendimento ASC, a.horaAtendimento ASC
            """)
    List<Atendimento> buscarAtivosComFiltros(@Param("pacienteId") Long pacienteId,
                                              @Param("profissionalId") Long profissionalId,
                                              @Param("data") LocalDate data,
                                              @Param("dataInicio") LocalDate dataInicio,
                                              @Param("dataFim") LocalDate dataFim,
                                              @Param("status") StatusAtendimento status,
                                              @Param("busca") String busca);
}
