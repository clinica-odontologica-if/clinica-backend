package com.clinica.servico_autenticacao.security;

import com.clinica.servico_autenticacao.model.Usuario;
import io.jsonwebtoken.Claims;
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

    private static final String TYPE_USER    = "user";
    private static final String TYPE_SERVICE = "service";

    private final Key    chave;
    private final long   expiracaoMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiracao-ms:28800000}") long expiracaoMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret não configurado. Verifique o Config Server e a variável JWT_SECRET.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret deve ter no mínimo 32 caracteres (256 bits para HS256).");
        }

        this.chave       = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
        log.info("JwtUtil inicializado com chave externa. Expiração: {}ms", expiracaoMs);
    }

    // -------------------------------------------------------------------------
    // Geração de tokens
    // -------------------------------------------------------------------------

    /**
     * Gera um token JWT para um usuário autenticado (login).
     * Claims: type=user, role, id.
     */
    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("type", TYPE_USER)
                .claim("role", usuario.getRole().name())
                .claim("id",   usuario.getId())
                .claim("nome", usuario.getNome())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiracaoMs))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Gera um token JWT para comunicação entre serviços (service-to-service).
     * Claims: type=service, service=<nomeServico>.
     * Expiração curta — 5 minutos — pois é usado apenas para uma chamada pontual.
     */
    public String gerarTokenServico(String nomeServico) {
        long expiracaoServico = 5 * 60 * 1000L; // 5 minutos
        return Jwts.builder()
                .setSubject(nomeServico)
                .claim("type",    TYPE_SERVICE)
                .claim("service", nomeServico)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiracaoServico))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Extração de claims
    // -------------------------------------------------------------------------

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    /**
     * Extrai o tipo do token: "user" ou "service".
     */
    public String extrairType(String token) {
        return extrairClaims(token).get("type", String.class);
    }

    /**
     * Extrai o nome do serviço de um token de serviço.
     */
    public String extrairNomeServico(String token) {
        return extrairClaims(token).get("service", String.class);
    }

    // -------------------------------------------------------------------------
    // Validação
    // -------------------------------------------------------------------------

    /**
     * Valida assinatura e expiração do token.
     */
    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se o token é do tipo "service".
     */
    public boolean isTokenDeServico(String token) {
        try {
            return TYPE_SERVICE.equals(extrairType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se o token é do tipo "user".
     */
    public boolean isTokenDeUsuario(String token) {
        try {
            return TYPE_USER.equals(extrairType(token));
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Interno
    // -------------------------------------------------------------------------

    private Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
