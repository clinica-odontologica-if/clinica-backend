package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.dto.RelatorioFinanceiroResponse;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.repository.DespesaRepository;
import com.clinica.servico_financeiro.repository.ReceitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioFinanceiroService")
class RelatorioFinanceiroServiceTest {

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private DespesaRepository despesaRepository;

    @InjectMocks
    private RelatorioFinanceiroService relatorioFinanceiroService;

    @Test
    @DisplayName("deve gerar totais e agrupamentos")
    void deveGerarRelatorio() {
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);
        when(receitaRepository.buscarParaRelatorio(true, inicio, fim)).thenReturn(List.of(
                receita(FormaPagamento.PIX, BigDecimal.valueOf(100)),
                receita(FormaPagamento.PIX, BigDecimal.valueOf(50)),
                receita(FormaPagamento.CARTAO_CREDITO, BigDecimal.valueOf(200))
        ));
        when(despesaRepository.buscarParaRelatorio(true, inicio, fim)).thenReturn(List.of(
                despesa(CategoriaDespesa.MATERIAL, BigDecimal.valueOf(80)),
                despesa(CategoriaDespesa.ALUGUEL, BigDecimal.valueOf(120))
        ));

        RelatorioFinanceiroResponse response = relatorioFinanceiroService.gerar(inicio, fim);

        assertThat(response.totalReceitas()).isEqualByComparingTo("350");
        assertThat(response.totalDespesas()).isEqualByComparingTo("200");
        assertThat(response.saldo()).isEqualByComparingTo("150");
        assertThat(response.receitasPorFormaPagamento().get(FormaPagamento.PIX)).isEqualByComparingTo("150");
        assertThat(response.despesasPorCategoria().get(CategoriaDespesa.ALUGUEL)).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("deve bloquear periodo invalido")
    void deveBloquearPeriodoInvalido() {
        assertThatThrownBy(() -> relatorioFinanceiroService.gerar(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Data inicial nao pode ser posterior a data final");
    }

    private Receita receita(FormaPagamento formaPagamento, BigDecimal valor) {
        Receita receita = new Receita();
        receita.setFormaPagamento(formaPagamento);
        receita.setValor(valor);
        return receita;
    }

    private Despesa despesa(CategoriaDespesa categoria, BigDecimal valor) {
        Despesa despesa = new Despesa();
        despesa.setCategoria(categoria);
        despesa.setValor(valor);
        return despesa;
    }
}