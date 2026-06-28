package com.clinica.servico_paciente.controller;

import com.clinica.servico_paciente.dto.PacienteRequest;
import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.security.JwtUtil;
import com.clinica.servico_paciente.service.PacienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PacienteControllerTest.MethodSecurityTestConfig.class)
@DisplayName("PacienteController")
class PacienteControllerTest {

    @MockitoBean
    private PacienteService pacienteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    PacienteControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve listar pacientes com filtros")
    void deveListarPacientesComFiltros() throws Exception {
        mockMvc.perform(get("/pacientes")
                        .param("busca", "maria")
                        .param("cpf", "123.456.789-01"))
                .andExpect(status().isOk());

        verify(pacienteService).listarAtivos("maria", "123.456.789-01");
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve bloquear cadastro quando usuario nao for gerente nem atendente")
    void deveBloquearCadastroQuandoUsuarioNaoForGerenteNemAtendente() throws Exception {
        PacienteRequest request = pacienteRequest();

        mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(pacienteService, never()).cadastrar(any());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve permitir cadastro quando usuario for atendente")
    void devePermitirCadastroQuandoUsuarioForAtendente() throws Exception {
        PacienteRequest request = pacienteRequest();
        PacienteResponse response = pacienteResponse();

        when(pacienteService.cadastrar(any(PacienteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(pacienteService).cadastrar(any(PacienteRequest.class));
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve bloquear edicao quando usuario nao for gerente nem atendente")
    void deveBloquearEdicaoQuandoUsuarioNaoForGerenteNemAtendente() throws Exception {
        PacienteRequest request = pacienteRequest();

        mockMvc.perform(put("/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(pacienteService, never()).atualizar(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve permitir edicao quando usuario for gerente")
    void devePermitirEdicaoQuandoUsuarioForGerente() throws Exception {
        PacienteRequest request = pacienteRequest();
        PacienteResponse response = pacienteResponse();

        when(pacienteService.atualizar(eq(1L), any(PacienteRequest.class))).thenReturn(response);

        mockMvc.perform(put("/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(pacienteService).atualizar(eq(1L), any(PacienteRequest.class));
    }

    private PacienteRequest pacienteRequest() {
        return new PacienteRequest(
                "Maria Silva",
                "12345678901",
                LocalDate.of(1990, 1, 10),
                "31999990000",
                "maria@clinica.com",
                "Rua A",
                "Sem observacoes"
        );
    }

    private PacienteResponse pacienteResponse() {
        return new PacienteResponse(
                1L,
                "Maria Silva",
                "12345678901",
                LocalDate.of(1990, 1, 10),
                "maria@clinica.com",
                "31999990000",
                "Rua A",
                "Sem observacoes",
                true,
                null
        );
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
