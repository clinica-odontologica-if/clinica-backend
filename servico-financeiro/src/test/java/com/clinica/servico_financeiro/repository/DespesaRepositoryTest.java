package com.clinica.servico_financeiro.repository;

import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
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
@DisplayName("DespesaRepository")
class DespesaRepositoryTest {

    @Autowired
    private DespesaRepository despesaRepository;

    @Test
    @DisplayName("deve buscar despesas com filtros")
    void deveBuscarDespesasComFiltros() {
        despesaRepository.saveAndFlush(despesa(CategoriaDespesa.MATERIAL, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 10), true));
        despesaRepository.saveAndFlush(despesa(CategoriaDespesa.ALUGUEL, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 11), true));
        despesaRepository.saveAndFlush(despesa(CategoriaDespesa.MATERIAL, StatusFinanceiro.PENDENTE, LocalDate.of(2026, 8, 1), true));

        List<Despesa> encontradas = despesaRepository.buscarComFiltros(
                true, CategoriaDespesa.MATERIAL, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.getFirst().getCategoria()).isEqualTo(CategoriaDespesa.MATERIAL);
    }

    @Test
    @DisplayName("deve buscar despesas para relatorio ignorando canceladas")
    void deveBuscarDespesasParaRelatorio() {
        despesaRepository.saveAndFlush(despesa(CategoriaDespesa.MATERIAL, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 10), true));
        despesaRepository.saveAndFlush(despesa(CategoriaDespesa.ALUGUEL, StatusFinanceiro.CANCELADO, LocalDate.of(2026, 7, 10), true));

        List<Despesa> encontradas = despesaRepository.buscarParaRelatorio(true, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.getFirst().getCategoria()).isEqualTo(CategoriaDespesa.MATERIAL);
    }

    private Despesa despesa(CategoriaDespesa categoria, StatusFinanceiro status, LocalDate data, boolean ativo) {
        Despesa despesa = new Despesa();
        despesa.setDescricao("Despesa " + categoria);
        despesa.setCategoria(categoria);
        despesa.setValor(BigDecimal.valueOf(100));
        despesa.setStatus(status);
        despesa.setDataPagamento(status == StatusFinanceiro.PAGO ? data : null);
        despesa.setDataVencimento(data);
        despesa.setAtivo(ativo);
        return despesa;
    }
}