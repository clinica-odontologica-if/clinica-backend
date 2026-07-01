package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.dto.DespesaRequest;
import com.clinica.servico_financeiro.dto.DespesaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.exception.RecursoNaoEncontradoException;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;

    @Transactional
    public DespesaResponse cadastrar(DespesaRequest request) {
        validarValor(request.valor());
        validarStatus(request.status(), request.dataPagamento());

        Despesa despesa = new Despesa();
        despesa.setDescricao(normalizarTextoObrigatorio(request.descricao(), "Descricao e obrigatoria"));
        despesa.setCategoria(request.categoria());
        despesa.setValor(request.valor());
        despesa.setStatus(resolverStatus(request.status()));
        despesa.setDataVencimento(request.dataVencimento());
        despesa.setDataPagamento(request.dataPagamento());
        despesa.setAtivo(true);

        return DespesaResponse.from(despesaRepository.save(despesa));
    }

    @Transactional(readOnly = true)
    public List<DespesaResponse> listar(CategoriaDespesa categoria, StatusFinanceiro status,
                                        LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodoOpcional(dataInicio, dataFim);
        return despesaRepository.buscarComFiltros(true, categoria, status, dataInicio, dataFim)
                .stream()
                .map(DespesaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DespesaResponse buscarPorId(Long id) {
        return DespesaResponse.from(buscarDespesaAtiva(id));
    }

    @Transactional
    public DespesaResponse atualizarStatus(Long id, StatusFinanceiroRequest request) {
        Despesa despesa = buscarDespesaAtiva(id);
        validarTransicaoStatus(despesa.getStatus(), request.status());
        validarStatus(request.status(), request.dataPagamento());

        despesa.setStatus(request.status());
        despesa.setDataPagamento(request.status() == StatusFinanceiro.PAGO ? request.dataPagamento() : null);

        return DespesaResponse.from(despesaRepository.save(despesa));
    }

    @Transactional
    public void inativar(Long id) {
        Despesa despesa = buscarDespesaAtiva(id);
        despesa.setStatus(StatusFinanceiro.CANCELADO);
        despesa.setAtivo(false);
        despesaRepository.save(despesa);
    }

    private Despesa buscarDespesaAtiva(Long id) {
        return despesaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa nao encontrada"));
    }

    private void validarStatus(StatusFinanceiro status, LocalDate dataPagamento) {
        StatusFinanceiro statusResolvido = resolverStatus(status);
        if (statusResolvido == StatusFinanceiro.PAGO && dataPagamento == null) {
            throw new RegraDeNegocioException("Data de pagamento e obrigatoria para status pago");
        }
    }

    private void validarTransicaoStatus(StatusFinanceiro statusAtual, StatusFinanceiro novoStatus) {
        if (novoStatus == null) {
            throw new RegraDeNegocioException("Status e obrigatorio");
        }
        if (statusAtual == StatusFinanceiro.CANCELADO && novoStatus != StatusFinanceiro.CANCELADO) {
            throw new RegraDeNegocioException("Status cancelado nao pode ser reaberto");
        }
    }

    private StatusFinanceiro resolverStatus(StatusFinanceiro status) {
        return status != null ? status : StatusFinanceiro.PENDENTE;
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor deve ser maior que zero");
        }
    }

    private void validarPeriodoOpcional(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new RegraDeNegocioException("Data inicial nao pode ser posterior a data final");
        }
    }

    private String normalizarTextoObrigatorio(String valor, String mensagem) {
        String texto = normalizarTextoOpcional(valor);
        if (texto == null) {
            throw new RegraDeNegocioException(mensagem);
        }
        return texto;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}