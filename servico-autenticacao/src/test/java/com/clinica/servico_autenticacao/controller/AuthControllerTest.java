package com.clinica.servico_autenticacao.controller;

import com.clinica.servico_autenticacao.MySQLContainerBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController — testes de integração")
class AuthControllerTest extends MySQLContainerBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Hash BCrypt corrigido pela migration V3.
    private static final String SENHA_PADRAO = "Gerente@2025";

    @Test
    @DisplayName("POST /auth/login deve retornar 200 e token JWT com credenciais corretas")
    void deveRetornarTokenComCredenciaisCorretas() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "gerente@clinica.com",
                "senha", SENHA_PADRAO
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 401 quando email não existe")
    void deveRetornar401QuandoEmailNaoExiste() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "naoexiste@clinica.com",
                "senha", SENHA_PADRAO
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 401 quando senha está incorreta")
    void deveRetornar401QuandoSenhaIncorreta() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "gerente@clinica.com",
                "senha", "senhaErrada"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/health deve retornar 200 sem autenticação")
    void deveRetornar200NoHealthCheck() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
