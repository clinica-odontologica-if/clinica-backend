package com.clinica.servico_atendimento.controller;

import com.clinica.servico_atendimento.dto.AtendimentoResponse;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import com.clinica.servico_atendimento.security.JwtFilter;
import com.clinica.servico_atendimento.security.JwtUtil;
import com.clinica.servico_atendimento.security.SecurityConfig;
import com.clinica.servico_atendimento.service.AtendimentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtendimentoController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtFilter.class})
@DisplayName("AtendimentoController")
class AtendimentoControllerTest {

    private final MockMvc mockMvc;

    @MockBean
    private AtendimentoService atendimentoService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    AtendimentoControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve cadastrar atendimento")
    void deveCadastrarAtendimento() throws Exception {
        when(atendimentoService.cadastrar(any(), eq("Bearer token"))).thenReturn(responsePadrao());

        mockMvc.perform(post("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 1,
                                  "profissionalId": 2,
                                  "data": "2099-01-10",
                                  "hora": "09:00",
                                  "observacoes": "Consulta inicial"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("AGENDADO"));
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve negar cadastro para dentista")
    void deveNegarCadastroParaDentista() throws Exception {
        mockMvc.perform(post("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 1,
                                  "profissionalId": 2,
                                  "data": "2099-01-10",
                                  "hora": "09:00"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve listar atendimentos para dentista")
    void deveListarAtendimentosParaDentista() throws Exception {
        when(atendimentoService.listar(eq(null), eq(null), eq(LocalDate.of(2099, 1, 10)), eq(null), eq(null), eq(StatusAtendimento.AGENDADO), eq(null), any(), eq("Bearer token")))
                .thenReturn(List.of(responsePadrao()));

        mockMvc.perform(get("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .param("data", "2099-01-10")
                        .param("status", "AGENDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].profissionalId").value(2));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve listar atendimentos com periodo e busca")
    void deveListarAtendimentosComPeriodoEBusca() throws Exception {
        when(atendimentoService.listar(
                eq(null),
                eq(null),
                eq(null),
                eq(LocalDate.of(2099, 1, 1)),
                eq(LocalDate.of(2099, 1, 31)),
                eq(StatusAtendimento.REALIZADO),
                eq("Maria"),
                any(),
                eq("Bearer token")
        )).thenReturn(List.of(responseRealizado()));

        mockMvc.perform(get("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .param("dataInicio", "2099-01-01")
                        .param("dataFim", "2099-01-31")
                        .param("status", "REALIZADO")
                        .param("busca", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("Maria Silva"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve atualizar status")
    void deveAtualizarStatus() throws Exception {
        AtendimentoResponse response = new AtendimentoResponse(
                10L,
                1L,
                "Maria Silva",
                2L,
                "Dr Joao",
                "joao@clinica.com",
                LocalDate.of(2099, 1, 10),
                LocalTime.of(9, 0),
                StatusAtendimento.REALIZADO,
                "Consulta inicial",
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(atendimentoService.atualizarStatus(eq(10L), any(), any(), eq("Bearer token"))).thenReturn(response);

        mockMvc.perform(patch("/atendimentos/10/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REALIZADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADO"));
    }


    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve buscar atendimento por id")
    void deveBuscarAtendimentoPorId() throws Exception {
        when(atendimentoService.buscarPorId(eq(10L), any(), eq("Bearer token"))).thenReturn(responsePadrao());

        mockMvc.perform(get("/atendimentos/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve realizar atendimento")
    void deveRealizarAtendimento() throws Exception {
        AtendimentoResponse response = responseRealizado();
        when(atendimentoService.realizar(eq(10L), any(), any(), eq("Bearer token"))).thenReturn(response);

        mockMvc.perform(patch("/atendimentos/10/realizar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "procedimentoRealizado": "Profilaxia",
                                  "observacoes": "Paciente sem queixas",
                                  "valor": 150.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADO"))
                .andExpect(jsonPath("$.procedimentoRealizado").value("Profilaxia"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve cancelar atendimento")
    void deveCancelarAtendimento() throws Exception {
        AtendimentoResponse response = new AtendimentoResponse(
                10L,
                1L,
                "Maria Silva",
                2L,
                "Dr Joao",
                "joao@clinica.com",
                LocalDate.of(2099, 1, 10),
                LocalTime.of(9, 0),
                StatusAtendimento.CANCELADO,
                "Consulta inicial",
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        when(atendimentoService.cancelar(eq(10L), any(), eq("Bearer token"))).thenReturn(response);

        mockMvc.perform(patch("/atendimentos/10/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    private AtendimentoResponse responseRealizado() {
        return new AtendimentoResponse(
                10L,
                1L,
                "Maria Silva",
                2L,
                "Dr Joao",
                "joao@clinica.com",
                LocalDate.of(2099, 1, 10),
                LocalTime.of(9, 0),
                StatusAtendimento.REALIZADO,
                "Paciente sem queixas",
                "Profilaxia",
                java.math.BigDecimal.valueOf(150),
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
    private AtendimentoResponse responsePadrao() {
        return new AtendimentoResponse(
                10L,
                1L,
                "Maria Silva",
                2L,
                "Dr Joao",
                "joao@clinica.com",
                LocalDate.of(2099, 1, 10),
                LocalTime.of(9, 0),
                StatusAtendimento.AGENDADO,
                "Consulta inicial",
                null,
                null,
                true,
                LocalDateTime.now(),
                null,
                null
        );
    }
}
