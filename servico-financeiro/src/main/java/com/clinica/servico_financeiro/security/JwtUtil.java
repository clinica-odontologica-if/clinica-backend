package com.clinica.servico_financeiro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Slf4j
@Component
public class JwtUtil {

    private final Key chave;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    public Long extrairId(String token) {
        Object id = extrairClaims(token).get("id");

        if (id instanceof Number numero) {
            return numero.longValue();
        }

        if (id instanceof String texto && !texto.isBlank()) {
            return Long.parseLong(texto);
        }

        return null;
    }

    public String extrairNome(String token) {
        return extrairClaims(token).get("nome", String.class);
    }

    public String extrairType(String token) {
        return extrairClaims(token).get("type", String.class);
    }

    public boolean isTokenDeUsuario(String token) {
        try {
            String type = extrairType(token);
            return type == null || "user".equals(type);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Token JWT invalido: {}", exception.getMessage());
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
