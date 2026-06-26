package com.clinica.servico_autenticacao.security.strategy;

import com.clinica.servico_autenticacao.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Estratégia para tokens de usuário autenticado (type=user).
 *
 * Monta o Authentication com:
 * - principal: email do usuário
 * - authorities: ROLE_<role> (ex: ROLE_GERENTE, ROLE_DENTISTA)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTokenStrategy implements TokenAuthenticationStrategy {

    private static final String TYPE_USER = "user";

    private final JwtUtil jwtUtil;

    @Override
    public boolean suporta(String type) {
        return TYPE_USER.equals(type);
    }

    @Override
    public Authentication autenticar(String token) {
        String email = jwtUtil.extrairEmail(token);
        String role  = jwtUtil.extrairRole(token);

        log.debug("Autenticando usuário via token JWT. Email: {}, Role: {}", email, role);

        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}