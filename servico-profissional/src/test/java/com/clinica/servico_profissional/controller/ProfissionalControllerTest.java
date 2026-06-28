package com.clinica.servico_profissional.controller;

import com.clinica.servico_profissional.dto.ProfissionalRequest;
import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.model.Role;
import com.clinica.servico_profissional.security.JwtUtil;
import com.clinica.servico_profissional.service.ProfissionalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfissionalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProfissionalControllerTest.MethodSecurityTestConfig.class)
@DisplayName("ProfissionalController")
class ProfissionalControllerTest {

    @MockitoBean
    private ProfissionalService profissionalService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    ProfissionalControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve bloquear cadastro quando usuario nao for gerente")
    void deveBloquearCadastroQuandoUsuarioNaoForGerente() throws Exception {
        ProfissionalRequest request = new ProfissionalRequest(
                "Ana",
                "ana@clinica.com",
                null,
                null,
                Role.ATENDENTE
        );

        mockMvc.perform(post("/profissionais")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(profissionalService, never()).cadastrar(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve permitir cadastro quando usuario for gerente")
    void devePermitirCadastroQuandoUsuarioForGerente() throws Exception {
        ProfissionalRequest request = new ProfissionalRequest(
                "Ana",
                "ana@clinica.com",
                null,
                null,
                Role.ATENDENTE
        );
        ProfissionalResponse response = new ProfissionalResponse(
                1L,
                "Ana",
                "ana@clinica.com",
                null,
                null,
                Role.ATENDENTE,
                true
        );

        when(profissionalService.cadastrar(any(ProfissionalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/profissionais")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(profissionalService).cadastrar(any(ProfissionalRequest.class));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
