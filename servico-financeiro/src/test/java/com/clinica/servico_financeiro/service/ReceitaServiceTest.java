package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.client.AtendimentoClient;
import com.clinica.servico_financeiro.client.AtendimentoClientResponse;
import com.clinica.servico_financeiro.dto.ReceitaRequest;
import com.clinica.servico_financeiro.dto.ReceitaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.Receita;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.repository.ReceitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceitaService")
class ReceitaServiceTest {

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private AtendimentoClient atendimentoClient;

    @InjectMocks
    private ReceitaService receitaService;

    @Test
    @DisplayName("deve cadastrar receita copiando dados do atendimento")
    void deveCadastrarReceita() {
        ReceitaRequest request = new ReceitaRequest(10L, " Consulta ", BigDecimal.valueOf(150),
                FormaPagamento.PIX, StatusFinanceiro.PAGO, LocalDate.now(), LocalDate.now());
        when(receitaRepository.existsByAtendimentoIdAndAtivoTrue(10L)).thenReturn(false);
        when(atendimentoClient.buscarPorId(10L, "Bearer token")).thenReturn(atendimento("REALIZADO", true));
        when(receitaRepository.save(any(Receita.class))).thenAnswer(invocation -> {
            Receita receita = invocation.getArgument(0);
            receita.setId(1L);
            return receita;
        });

        ReceitaResponse response = receitaService.cadastrar(request, "Bearer token");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.pacienteId()).isEqualTo(20L);
        assertThat(response.profissionalId()).isEqualTo(30L);
        assertThat(response.status()).isEqualTo(StatusFinanceiro.PAGO);
    }

    @Test
    @DisplayName("deve bloquear receita duplicada para atendimento")
    void deveBloquearReceitaDuplicada() {
        ReceitaRequest request = requestPendente();
        when(receitaRepository.existsByAtendimentoIdAndAtivoTrue(10L)).thenReturn(true);

        assertThatThrownBy(() -> receitaService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Ja existe receita ativa para este atendimento");

        verify(atendimentoClient, never()).buscarPorId(any(), any());
    }

    @Test
    @DisplayName("deve bloquear atendimento cancelado")
    void deveBloquearAtendimentoCancelado() {
        ReceitaRequest request = requestPendente();
        when(receitaRepository.existsByAtendimentoIdAndAtivoTrue(10L)).thenReturn(false);
        when(atendimentoClient.buscarPorId(10L, "Bearer token")).thenReturn(atendimento("CANCELADO", true));

        assertThatThrownBy(() -> receitaService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Atendimento cancelado ou nao comparecido nao pode gerar receita");
    }

    @Test
    @DisplayName("deve exigir atendimento realizado para receita paga")
    void deveExigirAtendimentoRealizadoParaReceitaPaga() {
        ReceitaRequest request = new ReceitaRequest(10L, "Consulta", BigDecimal.valueOf(150),
                FormaPagamento.PIX, StatusFinanceiro.PAGO, LocalDate.now(), LocalDate.now());
        when(receitaRepository.existsByAtendimentoIdAndAtivoTrue(10L)).thenReturn(false);
        when(atendimentoClient.buscarPorId(10L, "Bearer token")).thenReturn(atendimento("AGENDADO", true));

        assertThatThrownBy(() -> receitaService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Receita paga exige atendimento realizado");
    }

    @Test
    @DisplayName("deve atualizar status para pago exigindo data")
    void deveAtualizarStatusParaPago() {
        Receita receita = receitaSalva(StatusFinanceiro.PENDENTE);
        when(receitaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(receita));
        when(receitaRepository.save(receita)).thenReturn(receita);

        ReceitaResponse response = receitaService.atualizarStatus(
                1L, new StatusFinanceiroRequest(StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 1)));

        assertThat(response.status()).isEqualTo(StatusFinanceiro.PAGO);
        assertThat(response.dataPagamento()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    private ReceitaRequest requestPendente() {
        return new ReceitaRequest(10L, "Consulta", BigDecimal.valueOf(150),
                FormaPagamento.PIX, StatusFinanceiro.PENDENTE, LocalDate.now(), null);
    }

    private AtendimentoClientResponse atendimento(String status, boolean ativo) {
        return new AtendimentoClientResponse(10L, 20L, 30L, status, BigDecimal.valueOf(150), ativo);
    }

    private Receita receitaSalva(StatusFinanceiro status) {
        Receita receita = new Receita();
        receita.setId(1L);
        receita.setAtendimentoId(10L);
        receita.setPacienteId(20L);
        receita.setProfissionalId(30L);
        receita.setDescricao("Consulta");
        receita.setValor(BigDecimal.valueOf(150));
        receita.setFormaPagamento(FormaPagamento.PIX);
        receita.setStatus(status);
        receita.setAtivo(true);
        return receita;
    }
}