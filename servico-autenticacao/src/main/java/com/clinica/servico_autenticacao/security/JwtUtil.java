package com.clinica.servico_autenticacao.security;

import com.clinica.servico_autenticacao.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key chave = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRACAO = 1000 * 60 * 60 * 8; // 8 horas

    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("role", usuario.getRole().name())
                .claim("id", usuario.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRACAO))
                .signWith(chave)
                .compact();
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

    public boolean validarToken(String token) {
        try {
            extrairEmail(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
