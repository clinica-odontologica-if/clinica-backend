package com.clinica.servico_atendimento.repository;

import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    boolean existsByProfissionalIdAndDataAtendimentoAndHoraAtendimentoAndStatusInAndAtivoTrue(
            Long profissionalId,
            LocalDate dataAtendimento,
            LocalTime horaAtendimento,
            Collection<StatusAtendimento> status
    );

    @Query("""
            SELECT a
            FROM Atendimento a
            WHERE a.ativo = true
              AND (:pacienteId IS NULL OR a.pacienteId = :pacienteId)
              AND (:profissionalId IS NULL OR a.profissionalId = :profissionalId)
              AND (:data IS NULL OR a.dataAtendimento = :data)
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.dataAtendimento ASC, a.horaAtendimento ASC
            """)
    List<Atendimento> buscarAtivosComFiltros(@Param("pacienteId") Long pacienteId,
                                              @Param("profissionalId") Long profissionalId,
                                              @Param("data") LocalDate data,
                                              @Param("status") StatusAtendimento status);
}
