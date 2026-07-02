package com.clinica.servico_financeiro.controller;

import com.clinica.servico_financeiro.dto.DespesaRequest;
import com.clinica.servico_financeiro.dto.DespesaResponse;
import com.clinica.servico_financeiro.dto.ReceitaRequest;
import com.clinica.servico_financeiro.dto.ReceitaResponse;
import com.clinica.servico_financeiro.dto.RelatorioFinanceiroResponse;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.FormaPagamento;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.security.JwtUtil;
import com.clinica.servico_financeiro.service.DespesaService;
import com.clinica.servico_financeiro.service.ReceitaService;
import com.clinica.servico_financeiro.service.RelatorioFinanceiroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReceitaController.class, DespesaController.class, RelatorioFinanceiroController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(FinanceiroControllerTest.MethodSecurityTestConfig.class)
@DisplayName("Controllers financeiros")
class FinanceiroControllerTest {

    @MockitoBean
    private ReceitaService receitaService;

    @MockitoBean
    private DespesaService despesaService;

    @MockitoBean
    private RelatorioFinanceiroService relatorioFinanceiroService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    FinanceiroControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve permitir cadastro de receita para atendente")
    void devePermitirCadastroReceitaParaAtendente() throws Exception {
        when(receitaService.cadastrar(any(ReceitaRequest.class), eq("Bearer token"))).thenReturn(receitaResponse());

        mockMvc.perform(post("/receitas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receitaRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atendimentoId").value(10))
                .andExpect(jsonPath("$.status").value("PAGO"));

        verify(receitaService).cadastrar(any(ReceitaRequest.class), eq("Bearer token"));
    }

    @Test
    @WithMockUser(roles = "AUXILIAR")
    @DisplayName("deve bloquear cadastro de receita para auxiliar")
    void deveBloquearCadastroReceitaParaAuxiliar() throws Exception {
        mockMvc.perform(post("/receitas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receitaRequest())))
                .andExpect(status().isForbidden());

        verify(receitaService, never()).cadastrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve permitir cadastro de despesa para gerente")
    void devePermitirCadastroDespesaParaGerente() throws Exception {
        when(despesaService.cadastrar(any(DespesaRequest.class))).thenReturn(despesaResponse());

        mockMvc.perform(post("/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(despesaRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("MATERIAL"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve bloquear despesa para atendente")
    void deveBloquearDespesaParaAtendente() throws Exception {
        mockMvc.perform(post("/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(despesaRequest())))
                .andExpect(status().isForbidden());

        verify(despesaService, never()).cadastrar(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve gerar relatorio para gerente")
    void deveGerarRelatorioParaGerente() throws Exception {
        when(relatorioFinanceiroService.gerar(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new RelatorioFinanceiroResponse(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(200),
                        Map.of(FormaPagamento.PIX, BigDecimal.valueOf(300)),
                        Map.of(CategoriaDespesa.MATERIAL, BigDecimal.valueOf(100))
                ));

        mockMvc.perform(get("/relatorios/financeiro")
                        .param("dataInicio", "2026-07-01")
                        .param("dataFim", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(200));
    }

    private ReceitaRequest receitaRequest() {
        return new ReceitaRequest(10L, "Consulta", BigDecimal.valueOf(150), FormaPagamento.PIX,
                StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));
    }

    private ReceitaResponse receitaResponse() {
        return new ReceitaResponse(1L, 10L, 20L, 30L, "Consulta", BigDecimal.valueOf(150),
                FormaPagamento.PIX, StatusFinanceiro.PAGO, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 1), true, LocalDateTime.now(), null);
    }

    private DespesaRequest despesaRequest() {
        return new DespesaRequest("Material", CategoriaDespesa.MATERIAL, BigDecimal.valueOf(50),
                StatusFinanceiro.PENDENTE, LocalDate.of(2026, 7, 5), null);
    }

    private DespesaResponse despesaResponse() {
        return new DespesaResponse(1L, "Material", CategoriaDespesa.MATERIAL, BigDecimal.valueOf(50),
                StatusFinanceiro.PENDENTE, LocalDate.of(2026, 7, 5), null, true, LocalDateTime.now(), null);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}