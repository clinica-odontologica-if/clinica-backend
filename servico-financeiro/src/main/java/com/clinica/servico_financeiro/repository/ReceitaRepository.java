package com.clinica.servico_financeiro.repository;

import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    Optional<Receita> findByIdAndAtivoTrue(Long id);

    boolean existsByAtendimentoIdAndAtivoTrue(Long atendimentoId);

    @Query("""
            SELECT r
            FROM Receita r
            WHERE r.ativo = :ativo
              AND (:atendimentoId IS NULL OR r.atendimentoId = :atendimentoId)
              AND (:pacienteId IS NULL OR r.pacienteId = :pacienteId)
              AND (:profissionalId IS NULL OR r.profissionalId = :profissionalId)
              AND (:status IS NULL OR r.status = :status)
              AND (:dataInicio IS NULL OR COALESCE(r.dataPagamento, r.dataVencimento) >= :dataInicio)
              AND (:dataFim IS NULL OR COALESCE(r.dataPagamento, r.dataVencimento) <= :dataFim)
            ORDER BY r.criadoEm DESC
            """)
    List<Receita> buscarComFiltros(@Param("ativo") Boolean ativo,
                                   @Param("atendimentoId") Long atendimentoId,
                                   @Param("pacienteId") Long pacienteId,
                                   @Param("profissionalId") Long profissionalId,
                                   @Param("status") StatusFinanceiro status,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT r
            FROM Receita r
            WHERE r.ativo = :ativo
              AND r.status <> com.clinica.servico_financeiro.model.StatusFinanceiro.CANCELADO
              AND COALESCE(r.dataPagamento, r.dataVencimento) BETWEEN :dataInicio AND :dataFim
            """)
    List<Receita> buscarParaRelatorio(@Param("ativo") Boolean ativo,
                                      @Param("dataInicio") LocalDate dataInicio,
                                      @Param("dataFim") LocalDate dataFim);
}