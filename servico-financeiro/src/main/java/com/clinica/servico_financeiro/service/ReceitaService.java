package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.client.AtendimentoClient;
import com.clinica.servico_financeiro.client.AtendimentoClientResponse;
import com.clinica.servico_financeiro.dto.ReceitaRequest;
import com.clinica.servico_financeiro.dto.ReceitaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.exception.RecursoNaoEncontradoException;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final AtendimentoClient atendimentoClient;

    @Transactional
    public ReceitaResponse cadastrar(ReceitaRequest request, String authorizationHeader) {
        validarValor(request.valor());
        validarStatusInicial(request.status(), request.dataPagamento());

        if (receitaRepository.existsByAtendimentoIdAndAtivoTrue(request.atendimentoId())) {
            throw new RegraDeNegocioException("Ja existe receita ativa para este atendimento");
        }

        AtendimentoClientResponse atendimento = atendimentoClient.buscarPorId(
                request.atendimentoId(), resolverAuthorizationHeader(authorizationHeader));
        validarAtendimentoParaReceita(atendimento, request.status());

        Receita receita = new Receita();
        receita.setAtendimentoId(atendimento.id());
        receita.setPacienteId(atendimento.pacienteId());
        receita.setProfissionalId(atendimento.profissionalId());
        receita.setDescricao(resolverDescricao(request.descricao()));
        receita.setValor(request.valor());
        receita.setFormaPagamento(request.formaPagamento());
        receita.setStatus(resolverStatus(request.status()));
        receita.setDataVencimento(request.dataVencimento());
        receita.setDataPagamento(request.dataPagamento());
        receita.setAtivo(true);

        return ReceitaResponse.from(receitaRepository.save(receita));
    }

    @Transactional(readOnly = true)
    public List<ReceitaResponse> listar(Long atendimentoId, Long pacienteId, Long profissionalId,
                                        StatusFinanceiro status, LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodoOpcional(dataInicio, dataFim);
        return receitaRepository.buscarComFiltros(true, atendimentoId, pacienteId, profissionalId, status, dataInicio, dataFim)
                .stream()
                .map(ReceitaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceitaResponse buscarPorId(Long id) {
        return ReceitaResponse.from(buscarReceitaAtiva(id));
    }

    @Transactional
    public ReceitaResponse atualizarStatus(Long id, StatusFinanceiroRequest request) {
        Receita receita = buscarReceitaAtiva(id);
        validarTransicaoStatus(receita.getStatus(), request.status());
        validarStatusInicial(request.status(), request.dataPagamento());

        receita.setStatus(request.status());
        receita.setDataPagamento(request.status() == StatusFinanceiro.PAGO ? request.dataPagamento() : null);

        return ReceitaResponse.from(receitaRepository.save(receita));
    }

    @Transactional
    public void inativar(Long id) {
        Receita receita = buscarReceitaAtiva(id);
        receita.setStatus(StatusFinanceiro.CANCELADO);
        receita.setAtivo(false);
        receitaRepository.save(receita);
    }

    private Receita buscarReceitaAtiva(Long id) {
        return receitaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Receita nao encontrada"));
    }

    private void validarAtendimentoParaReceita(AtendimentoClientResponse atendimento, StatusFinanceiro statusReceita) {
        if (atendimento == null || atendimento.id() == null) {
            throw new RecursoNaoEncontradoException("Atendimento nao encontrado");
        }

        if (!atendimento.ativo()) {
            throw new RegraDeNegocioException("Atendimento inativo nao pode gerar receita");
        }

        if ("CANCELADO".equals(atendimento.status()) || "NAO_COMPARECEU".equals(atendimento.status())) {
            throw new RegraDeNegocioException("Atendimento cancelado ou nao comparecido nao pode gerar receita");
        }

        if (statusReceita == StatusFinanceiro.PAGO && !"REALIZADO".equals(atendimento.status())) {
            throw new RegraDeNegocioException("Receita paga exige atendimento realizado");
        }
    }

    private void validarStatusInicial(StatusFinanceiro status, LocalDate dataPagamento) {
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

    private String resolverDescricao(String descricao) {
        String texto = normalizarTextoOpcional(descricao);
        return texto != null ? texto : "Pagamento de atendimento";
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

    private String resolverAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new RegraDeNegocioException("Token de autorizacao e obrigatorio para validar atendimento");
        }
        return authorizationHeader;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}