package com.clinica.servico_paciente.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private static final String TYPE_SERVICE = "service";

    private final Key chave;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret nao configurado. Verifique o Config Server.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("jwt.secret deve ter no minimo 32 caracteres (256 bits).");
        }
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtUtil inicializado com chave configurada externamente.");
    }

    public String extrairEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extrairRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public String extrairType(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("type", String.class);
    }

    public boolean isTokenDeUsuario(String token) {
        try {
            return "user".equals(extrairType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public String gerarTokenServico(String nomeServico) {
        long expiracaoServico = 5 * 60 * 1000L;

        return Jwts.builder()
                .setSubject(nomeServico)
                .claim("type", TYPE_SERVICE)
                .claim("service", nomeServico)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiracaoServico))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            extrairEmail(token);
            return true;
        } catch (Exception e) {
            log.warn("Token JWT invalido: {}", e.getMessage());
            return false;
        }
    }
}
