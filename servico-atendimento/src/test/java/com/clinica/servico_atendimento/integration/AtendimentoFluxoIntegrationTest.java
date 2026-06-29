package com.clinica.servico_atendimento.integration;

import com.clinica.servico_atendimento.client.PacienteClient;
import com.clinica.servico_atendimento.client.PacienteClientResponse;
import com.clinica.servico_atendimento.client.ProfissionalClient;
import com.clinica.servico_atendimento.client.ProfissionalClientResponse;
import com.clinica.servico_atendimento.repository.AtendimentoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Fluxo integrado de atendimentos")
class AtendimentoFluxoIntegrationTest {

    private static final String SECRET = "desenvolvimento-local-chave-minima-32-chars";
    private static final String TOKEN_GERENTE = "Bearer " + gerarToken("gerente@clinica.com", "GERENTE", 1L, "Gerente");
    private static final String TOKEN_DENTISTA = "Bearer " + gerarToken("dentista@clinica.com", "DENTISTA", 2L, "Dr Joao");
    private static final String TOKEN_OUTRO_DENTISTA = "Bearer " + gerarToken("outro@clinica.com", "DENTISTA", 3L, "Dra Outra");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @MockBean
    private PacienteClient pacienteClient;

    @MockBean
    private ProfissionalClient profissionalClient;

    @BeforeEach
    void setUp() {
        atendimentoRepository.deleteAll();

        when(pacienteClient.buscarPorId(eq(1L), anyString()))
                .thenReturn(new PacienteClientResponse(1L, "Maria Silva", "12345678900", "maria@clinica.com", "31999999999", true));
        when(profissionalClient.buscarPorId(eq(2L), anyString()))
                .thenReturn(new ProfissionalClientResponse(2L, "Dr Joao", "joao@clinica.com", "CRO-1234", "Ortodontia", "DENTISTA", true));
        when(profissionalClient.buscarMeuPerfil(TOKEN_DENTISTA))
                .thenReturn(new ProfissionalClientResponse(2L, "Dr Joao", "joao@clinica.com", "CRO-1234", "Ortodontia", "DENTISTA", true));
        when(profissionalClient.buscarMeuPerfil(TOKEN_OUTRO_DENTISTA))
                .thenReturn(new ProfissionalClientResponse(3L, "Dra Outra", "outra@clinica.com", "CRO-5678", "Endodontia", "DENTISTA", true));
    }

    @Test
    @DisplayName("deve executar fluxo de criar, listar, confirmar, realizar e buscar")
    void deveExecutarFluxoCompletoDeAtendimento() throws Exception {
        Long atendimentoId = criarAtendimento("2099-01-10", "09:00");

        mockMvc.perform(get("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_GERENTE)
                        .param("data", "2099-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(atendimentoId))
                .andExpect(jsonPath("$[0].status").value("AGENDADO"));

        mockMvc.perform(patch("/atendimentos/{id}/status", atendimentoId)
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_GERENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        mockMvc.perform(patch("/atendimentos/{id}/realizar", atendimentoId)
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_DENTISTA)
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
                .andExpect(jsonPath("$.procedimentoRealizado").value("Profilaxia"))
                .andExpect(jsonPath("$.valor").value(150.00));

        mockMvc.perform(get("/atendimentos/{id}", atendimentoId)
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_DENTISTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(atendimentoId))
                .andExpect(jsonPath("$.status").value("REALIZADO"))
                .andExpect(jsonPath("$.realizadoEm").exists());
    }

    @Test
    @DisplayName("deve impedir conflito de horario no fluxo HTTP")
    void deveImpedirConflitoDeHorarioNoFluxoHttp() throws Exception {
        criarAtendimento("2099-01-11", "10:00");

        mockMvc.perform(post("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_GERENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 1,
                                  "profissionalId": 2,
                                  "data": "2099-01-11",
                                  "hora": "10:00",
                                  "observacoes": "Retorno"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Profissional ja possui atendimento agendado nesse horario"));
    }

    @Test
    @DisplayName("deve restringir dentista a seus proprios atendimentos no fluxo HTTP")
    void deveRestringirDentistaAosPropriosAtendimentosNoFluxoHttp() throws Exception {
        Long atendimentoId = criarAtendimento("2099-01-12", "11:00");

        mockMvc.perform(get("/atendimentos/{id}", atendimentoId)
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_OUTRO_DENTISTA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Acesso negado"));
    }

    @Test
    @DisplayName("deve cancelar atendimento agendado no fluxo HTTP")
    void deveCancelarAtendimentoAgendadoNoFluxoHttp() throws Exception {
        Long atendimentoId = criarAtendimento("2099-01-13", "14:00");

        mockMvc.perform(patch("/atendimentos/{id}/cancelar", atendimentoId)
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_GERENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    private Long criarAtendimento(String data, String hora) throws Exception {
        String response = mockMvc.perform(post("/atendimentos")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_GERENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 1,
                                  "profissionalId": 2,
                                  "data": "%s",
                                  "hora": "%s",
                                  "observacoes": "Consulta inicial"
                                }
                                """.formatted(data, hora)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String marker = "\"id\":";
        int inicio = response.indexOf(marker) + marker.length();
        int fim = response.indexOf(',', inicio);
        return Long.parseLong(response.substring(inicio, fim));
    }

    private static String gerarToken(String email, String role, Long id, String nome) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + 3_600_000);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("id", id)
                .claim("nome", nome)
                .claim("type", "user")
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
