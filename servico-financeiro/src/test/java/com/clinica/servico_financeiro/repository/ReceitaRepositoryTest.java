package com.clinica.servico_financeiro.repository;

import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ReceitaRepository")
class ReceitaRepositoryTest {

    @Autowired
    private ReceitaRepository receitaRepository;

    @Test
    @DisplayName("deve buscar receitas com filtros")
    void deveBuscarReceitasComFiltros() {
        receitaRepository.saveAndFlush(receita(1L, 10L, 20L, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 10), true));
        receitaRepository.saveAndFlush(receita(2L, 11L, 20L, StatusFinanceiro.PENDENTE, LocalDate.of(2026, 7, 11), true));
        receitaRepository.saveAndFlush(receita(3L, 10L, 21L, StatusFinanceiro.PAGO, LocalDate.of(2026, 8, 1), false));

        List<Receita> encontradas = receitaRepository.buscarComFiltros(
                true, null, 10L, 20L, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.getFirst().getAtendimentoId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve buscar receitas para relatorio ignorando canceladas")
    void deveBuscarReceitasParaRelatorio() {
        receitaRepository.saveAndFlush(receita(1L, 10L, 20L, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 10), true));
        receitaRepository.saveAndFlush(receita(2L, 10L, 20L, StatusFinanceiro.CANCELADO, LocalDate.of(2026, 7, 11), true));

        List<Receita> encontradas = receitaRepository.buscarParaRelatorio(true, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.getFirst().getStatus()).isEqualTo(StatusFinanceiro.PAGO);
    }

    private Receita receita(Long atendimentoId, Long pacienteId, Long profissionalId,
                            StatusFinanceiro status, LocalDate data, boolean ativo) {
        Receita receita = new Receita();
        receita.setAtendimentoId(atendimentoId);
        receita.setPacienteId(pacienteId);
        receita.setProfissionalId(profissionalId);
        receita.setDescricao("Consulta");
        receita.setValor(BigDecimal.valueOf(100));
        receita.setFormaPagamento(FormaPagamento.PIX);
        receita.setStatus(status);
        receita.setDataPagamento(status == StatusFinanceiro.PAGO ? data : null);
        receita.setDataVencimento(data);
        receita.setAtivo(ativo);
        return receita;
    }
}