package com.clinica.servico_financeiro.service;

import com.clinica.servico_financeiro.dto.DespesaRequest;
import com.clinica.servico_financeiro.dto.DespesaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.exception.RegraDeNegocioException;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.Despesa;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.repository.DespesaRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DespesaService")
class DespesaServiceTest {

    @Mock
    private DespesaRepository despesaRepository;

    @InjectMocks
    private DespesaService despesaService;

    @Test
    @DisplayName("deve cadastrar despesa normalizando descricao")
    void deveCadastrarDespesa() {
        when(despesaRepository.save(any(Despesa.class))).thenAnswer(invocation -> {
            Despesa despesa = invocation.getArgument(0);
            despesa.setId(1L);
            return despesa;
        });

        DespesaResponse response = despesaService.cadastrar(new DespesaRequest(
                " Material odontologico ", CategoriaDespesa.MATERIAL, BigDecimal.valueOf(90),
                StatusFinanceiro.PENDENTE, LocalDate.now(), null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.descricao()).isEqualTo("Material odontologico");
        assertThat(response.status()).isEqualTo(StatusFinanceiro.PENDENTE);
    }

    @Test
    @DisplayName("deve bloquear despesa paga sem data de pagamento")
    void deveBloquearDespesaPagaSemData() {
        assertThatThrownBy(() -> despesaService.cadastrar(new DespesaRequest(
                "Aluguel", CategoriaDespesa.ALUGUEL, BigDecimal.valueOf(1000),
                StatusFinanceiro.PAGO, LocalDate.now(), null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Data de pagamento e obrigatoria para status pago");
    }

    @Test
    @DisplayName("deve bloquear reabertura de despesa cancelada")
    void deveBloquearReaberturaCancelada() {
        Despesa despesa = new Despesa();
        despesa.setId(1L);
        despesa.setDescricao("Aluguel");
        despesa.setCategoria(CategoriaDespesa.ALUGUEL);
        despesa.setValor(BigDecimal.valueOf(1000));
        despesa.setStatus(StatusFinanceiro.CANCELADO);
        despesa.setAtivo(true);
        when(despesaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(despesa));

        assertThatThrownBy(() -> despesaService.atualizarStatus(
                1L, new StatusFinanceiroRequest(StatusFinanceiro.PAGO, LocalDate.now())))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Status cancelado nao pode ser reaberto");
    }

    @Test
    @DisplayName("deve inativar despesa marcando cancelada")
    void deveInativarDespesa() {
        Despesa despesa = new Despesa();
        despesa.setId(1L);
        despesa.setStatus(StatusFinanceiro.PENDENTE);
        despesa.setAtivo(true);
        when(despesaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(despesa));

        despesaService.inativar(1L);

        assertThat(despesa.isAtivo()).isFalse();
        assertThat(despesa.getStatus()).isEqualTo(StatusFinanceiro.CANCELADO);
        verify(despesaRepository).save(despesa);
    }
}