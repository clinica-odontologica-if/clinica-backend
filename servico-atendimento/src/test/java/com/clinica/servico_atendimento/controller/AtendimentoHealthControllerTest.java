package com.clinica.servico_atendimento.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.clinica.servico_atendimento.security.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtendimentoHealthController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@DisplayName("AtendimentoHealthController")
class AtendimentoHealthControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    AtendimentoHealthControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("deve expor health sem autenticacao")
    void deveExporHealthSemAutenticacao() throws Exception {
        mockMvc.perform(get("/atendimentos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("servico-atendimento"));
    }
}
