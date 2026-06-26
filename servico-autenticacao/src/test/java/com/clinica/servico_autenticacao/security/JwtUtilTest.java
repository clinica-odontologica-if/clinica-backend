package com.clinica.servico_autenticacao.security;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil — testes unitários")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Usuario usuarioDentista;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        usuarioDentista = new Usuario(
                2L,
                "Dentista",
                "dentista@clinica.com",
                "hashSenha",
                Role.DENTISTA
        );
    }

    @Test
    @DisplayName("deve gerar token não nulo para usuário válido")
    void deveGerarTokenNaoNulo() {
        String token = jwtUtil.gerarToken(usuarioDentista);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("deve extrair o email correto do token gerado")
    void deveExtrairEmailDoToken() {
        String token = jwtUtil.gerarToken(usuarioDentista);

        String emailExtraido = jwtUtil.extrairEmail(token);

        assertThat(emailExtraido).isEqualTo("dentista@clinica.com");
    }

    @Test
    @DisplayName("deve extrair a role correta do token gerado")
    void deveExtrairRoleDoToken() {
        String token = jwtUtil.gerarToken(usuarioDentista);

        String roleExtraida = jwtUtil.extrairRole(token);

        assertThat(roleExtraida).isEqualTo("DENTISTA");
    }

    @Test
    @DisplayName("deve validar como verdadeiro um token gerado corretamente")
    void deveValidarTokenValido() {
        String token = jwtUtil.gerarToken(usuarioDentista);

        boolean valido = jwtUtil.validarToken(token);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("deve validar como falso um token inválido ou adulterado")
    void deveInvalidarTokenAdulterado() {
        String tokenInvalido = "eyJhbGciOiJIUzI1NiJ9.tokenAdulterado.assinatura_invalida";

        boolean valido = jwtUtil.validarToken(tokenInvalido);

        assertThat(valido).isFalse();
    }
}