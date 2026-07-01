package com.clinica.servico_atendimento.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "segredo-super-seguro-para-testes-atendimento-123456";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    @Test
    @DisplayName("deve extrair dados do token de usuario")
    void deveExtrairDadosDoTokenDeUsuario() {
        String token = gerarToken("user");

        assertThat(jwtUtil.validarToken(token)).isTrue();
        assertThat(jwtUtil.isTokenDeUsuario(token)).isTrue();
        assertThat(jwtUtil.extrairEmail(token)).isEqualTo("dentista@clinica.com");
        assertThat(jwtUtil.extrairRole(token)).isEqualTo("DENTISTA");
        assertThat(jwtUtil.extrairId(token)).isEqualTo(7L);
        assertThat(jwtUtil.extrairNome(token)).isEqualTo("Dra Ana");
    }

    @Test
    @DisplayName("deve rejeitar token invalido")
    void deveRejeitarTokenInvalido() {
        assertThat(jwtUtil.validarToken("token-invalido")).isFalse();
        assertThat(jwtUtil.isTokenDeUsuario("token-invalido")).isFalse();
    }

    @Test
    @DisplayName("deve identificar token que nao e de usuario")
    void deveIdentificarTokenQueNaoEDeUsuario() {
        String token = gerarToken("service");

        assertThat(jwtUtil.validarToken(token)).isTrue();
        assertThat(jwtUtil.isTokenDeUsuario(token)).isFalse();
    }

    private String gerarToken(String type) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + 60_000);

        return Jwts.builder()
                .setSubject("dentista@clinica.com")
                .claim("role", "DENTISTA")
                .claim("id", 7L)
                .claim("nome", "Dra Ana")
                .claim("type", type)
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
