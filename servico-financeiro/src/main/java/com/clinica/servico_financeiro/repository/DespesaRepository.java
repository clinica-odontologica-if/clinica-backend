package com.clinica.servico_financeiro.repository;

import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    Optional<Despesa> findByIdAndAtivoTrue(Long id);

    @Query("""
            SELECT d
            FROM Despesa d
            WHERE d.ativo = :ativo
              AND (:categoria IS NULL OR d.categoria = :categoria)
              AND (:status IS NULL OR d.status = :status)
              AND (:dataInicio IS NULL OR COALESCE(d.dataPagamento, d.dataVencimento) >= :dataInicio)
              AND (:dataFim IS NULL OR COALESCE(d.dataPagamento, d.dataVencimento) <= :dataFim)
            ORDER BY d.criadoEm DESC
            """)
    List<Despesa> buscarComFiltros(@Param("ativo") Boolean ativo,
                                   @Param("categoria") CategoriaDespesa categoria,
                                   @Param("status") StatusFinanceiro status,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT d
            FROM Despesa d
            WHERE d.ativo = :ativo
              AND d.status <> com.clinica.servico_financeiro.model.StatusFinanceiro.CANCELADO
              AND COALESCE(d.dataPagamento, d.dataVencimento) BETWEEN :dataInicio AND :dataFim
            """)
    List<Despesa> buscarParaRelatorio(@Param("ativo") Boolean ativo,
                                      @Param("dataInicio") LocalDate dataInicio,
                                      @Param("dataFim") LocalDate dataFim);
}