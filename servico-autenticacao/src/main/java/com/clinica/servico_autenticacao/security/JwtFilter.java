package com.clinica.servico_autenticacao.security;

import com.clinica.servico_autenticacao.security.strategy.TokenAuthenticationStrategy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de autenticação JWT executado uma vez por requisição.
 *
 * Responsabilidades:
 * - Extrair o token do header Authorization
 * - Validar assinatura e expiração via JwtUtil
 * - Delegar a montagem do Authentication para a estratégia correta
 * - Colocar o Authentication no SecurityContextHolder
 *
 * Não tem conhecimento dos tipos de token existentes — essa decisão
 * pertence às implementações de TokenAuthenticationStrategy.
 * Novos tipos de token = nova estratégia, sem modificar este filtro.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final List<TokenAuthenticationStrategy> strategies;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        extrairToken(request)
                .filter(this::tokenValido)
                .flatMap(this::resolverAutenticacao)
                .ifPresent(auth ->
                        SecurityContextHolder.getContext().setAuthentication(auth));

        filterChain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Privados
    // -------------------------------------------------------------------------

    private Optional<String> extrairToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }

    private boolean tokenValido(String token) {
        boolean valido = jwtUtil.validarToken(token);
        if (!valido) {
            log.warn("Token JWT rejeitado — assinatura inválida ou expirado.");
        }
        return valido;
    }

    private Optional<Authentication> resolverAutenticacao(String token) {
        String type = jwtUtil.extrairType(token);

        if (type == null) {
            log.warn("Token JWT sem claim 'type' — rejeitado.");
            return Optional.empty();
        }

        return strategies.stream()
                .filter(strategy -> strategy.suporta(type))
                .findFirst()
                .map(strategy -> strategy.autenticar(token))
                .or(() -> {
                    log.warn("Nenhuma estratégia encontrada para token do tipo '{}' — rejeitado.", type);
                    return Optional.empty();
                });
    }
}