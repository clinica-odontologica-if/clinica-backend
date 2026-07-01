package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.dto.RelatorioFinanceiroResponse;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.repository.DespesaRepository;
import com.clinica.servico_financeiro.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    private final ReceitaRepository receitaRepository;
    private final DespesaRepository despesaRepository;

    @Transactional(readOnly = true)
    public RelatorioFinanceiroResponse gerar(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);

        List<Receita> receitas = receitaRepository.buscarParaRelatorio(true, dataInicio, dataFim);
        List<Despesa> despesas = despesaRepository.buscarParaRelatorio(true, dataInicio, dataFim);

        BigDecimal totalReceitas = somarReceitas(receitas);
        BigDecimal totalDespesas = somarDespesas(despesas);

        return new RelatorioFinanceiroResponse(
                dataInicio,
                dataFim,
                totalReceitas,
                totalDespesas,
                totalReceitas.subtract(totalDespesas),
                agruparReceitasPorForma(receitas),
                agruparDespesasPorCategoria(despesas)
        );
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new RegraDeNegocioException("Data inicial e data final sao obrigatorias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new RegraDeNegocioException("Data inicial nao pode ser posterior a data final");
        }
    }

    private BigDecimal somarReceitas(List<Receita> receitas) {
        return receitas.stream()
                .map(Receita::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarDespesas(List<Despesa> despesas) {
        return despesas.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<FormaPagamento, BigDecimal> agruparReceitasPorForma(List<Receita> receitas) {
        Map<FormaPagamento, BigDecimal> agrupado = new EnumMap<>(FormaPagamento.class);
        receitas.forEach(receita -> agrupado.merge(receita.getFormaPagamento(), receita.getValor(), BigDecimal::add));
        return agrupado;
    }

    private Map<CategoriaDespesa, BigDecimal> agruparDespesasPorCategoria(List<Despesa> despesas) {
        Map<CategoriaDespesa, BigDecimal> agrupado = new EnumMap<>(CategoriaDespesa.class);
        despesas.forEach(despesa -> agrupado.merge(despesa.getCategoria(), despesa.getValor(), BigDecimal::add));
        return agrupado;
    }
}