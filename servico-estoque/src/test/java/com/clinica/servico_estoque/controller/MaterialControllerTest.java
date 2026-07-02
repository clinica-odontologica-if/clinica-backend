package com.clinica.servico_estoque.controller;

import com.clinica.servico_estoque.dto.MaterialRequest;
import com.clinica.servico_estoque.dto.MaterialResponse;
import com.clinica.servico_estoque.dto.MovimentacaoEstoqueRequest;
import com.clinica.servico_estoque.dto.MovimentacaoEstoqueResponse;
import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;
import com.clinica.servico_estoque.model.UnidadeMedida;
import com.clinica.servico_estoque.security.JwtUtil;
import com.clinica.servico_estoque.service.MaterialService;
import com.clinica.servico_estoque.service.MovimentacaoEstoqueService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MaterialControllerTest.MethodSecurityTestConfig.class)
@DisplayName("MaterialController")
class MaterialControllerTest {

    @MockitoBean
    private MaterialService materialService;

    @MockitoBean
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    MaterialControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("deve listar materiais com filtros")
    void deveListarMateriaisComFiltros() throws Exception {
        when(materialService.listar("anes", "Medicamento", true, null)).thenReturn(List.of(responsePadrao()));

        mockMvc.perform(get("/materiais")
                        .param("busca", "anes")
                        .param("categoria", "Medicamento")
                        .param("baixoEstoque", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Anestesico"));

        verify(materialService).listar("anes", "Medicamento", true, null);
    }

    @Test
    @WithMockUser(roles = "AUXILIAR")
    @DisplayName("deve listar alertas de baixo estoque")
    void deveListarAlertasDeBaixoEstoque() throws Exception {
        when(materialService.listarBaixoEstoque()).thenReturn(List.of(responsePadrao()));

        mockMvc.perform(get("/materiais/alertas/baixo-estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baixoEstoque").value(true));
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve bloquear cadastro para perfil sem permissao")
    void deveBloquearCadastroParaPerfilSemPermissao() throws Exception {
        mockMvc.perform(post("/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPadrao())))
                .andExpect(status().isForbidden());

        verify(materialService, never()).cadastrar(any());
    }

    @Test
    @WithMockUser(roles = "AUXILIAR")
    @DisplayName("deve permitir cadastro para auxiliar")
    void devePermitirCadastroParaAuxiliar() throws Exception {
        when(materialService.cadastrar(any(MaterialRequest.class))).thenReturn(responsePadrao());

        mockMvc.perform(post("/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPadrao())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Anestesico"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve permitir atualizacao para gerente")
    void devePermitirAtualizacaoParaGerente() throws Exception {
        when(materialService.atualizar(eq(1L), any(MaterialRequest.class))).thenReturn(responsePadrao());

        mockMvc.perform(put("/materiais/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPadrao())))
                .andExpect(status().isOk());

        verify(materialService).atualizar(eq(1L), any(MaterialRequest.class));
    }

    @Test
    @WithMockUser(roles = "AUXILIAR")
    @DisplayName("deve bloquear inativacao para auxiliar")
    void deveBloquearInativacaoParaAuxiliar() throws Exception {
        mockMvc.perform(patch("/materiais/1/inativar"))
                .andExpect(status().isForbidden());

        verify(materialService, never()).inativar(any());
    }

    @Test
    @WithMockUser(roles = "AUXILIAR")
    @DisplayName("deve registrar movimentacao para auxiliar")
    void deveRegistrarMovimentacaoParaAuxiliar() throws Exception {
        when(movimentacaoEstoqueService.registrar(eq(1L), any(MovimentacaoEstoqueRequest.class), any()))
                .thenReturn(movimentacaoResponsePadrao());

        mockMvc.perform(post("/materiais/1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentacaoRequestPadrao())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.saldoAtual").value(15));

        verify(movimentacaoEstoqueService).registrar(eq(1L), any(MovimentacaoEstoqueRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("deve listar movimentacoes para gerente")
    void deveListarMovimentacoesParaGerente() throws Exception {
        when(movimentacaoEstoqueService.listarPorMaterial(1L)).thenReturn(List.of(movimentacaoResponsePadrao()));

        mockMvc.perform(get("/materiais/1/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materialNome").value("Anestesico"));
    }

    @Test
    @WithMockUser(roles = "DENTISTA")
    @DisplayName("deve bloquear movimentacao para dentista")
    void deveBloquearMovimentacaoParaDentista() throws Exception {
        mockMvc.perform(post("/materiais/1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentacaoRequestPadrao())))
                .andExpect(status().isForbidden());

        verify(movimentacaoEstoqueService, never()).registrar(any(), any(), any());
    }

    private MaterialRequest requestPadrao() {
        return new MaterialRequest(
                "Anestesico",
                "Tubete odontologico",
                "Medicamento",
                UnidadeMedida.UNIDADE,
                BigDecimal.valueOf(5),
                BigDecimal.TEN
        );
    }

    private MaterialResponse responsePadrao() {
        return new MaterialResponse(
                1L,
                "Anestesico",
                "Tubete odontologico",
                "Medicamento",
                UnidadeMedida.UNIDADE,
                BigDecimal.valueOf(5),
                BigDecimal.TEN,
                true,
                true,
                LocalDateTime.now(),
                null
        );
    }

    private MovimentacaoEstoqueRequest movimentacaoRequestPadrao() {
        return new MovimentacaoEstoqueRequest(
                TipoMovimentacaoEstoque.ENTRADA,
                BigDecimal.valueOf(5),
                "Compra"
        );
    }

    private MovimentacaoEstoqueResponse movimentacaoResponsePadrao() {
        return new MovimentacaoEstoqueResponse(
                1L,
                1L,
                "Anestesico",
                TipoMovimentacaoEstoque.ENTRADA,
                BigDecimal.valueOf(5),
                BigDecimal.TEN,
                BigDecimal.valueOf(15),
                "Compra",
                "auxiliar@clinica.com",
                LocalDateTime.now()
        );
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
