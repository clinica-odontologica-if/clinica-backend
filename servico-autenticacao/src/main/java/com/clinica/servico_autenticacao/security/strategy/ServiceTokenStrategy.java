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
 * Estratégia para tokens de comunicação entre serviços (type=service).
 *
 * Monta o Authentication com:
 * - principal: nome do serviço chamador
 * - authorities: ROLE_SERVICE
 *
 * Tokens de serviço têm expiração curta (5 minutos) e são usados
 * exclusivamente para chamadas internas entre microsserviços.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTokenStrategy implements TokenAuthenticationStrategy {

    private static final String TYPE_SERVICE = "service";

    private final JwtUtil jwtUtil;

    @Override
    public boolean suporta(String type) {
        return TYPE_SERVICE.equals(type);
    }

    @Override
    public Authentication autenticar(String token) {
        String nomeServico = jwtUtil.extrairNomeServico(token);

        log.debug("Autenticando chamada de serviço via token JWT. Serviço: {}", nomeServico);

        return new UsernamePasswordAuthenticationToken(
                nomeServico,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
    }
}