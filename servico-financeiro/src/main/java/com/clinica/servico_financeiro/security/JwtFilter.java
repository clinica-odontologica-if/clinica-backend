package com.clinica.servico_financeiro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            autenticarToken(authorizationHeader.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarToken(String token, HttpServletRequest request) {
        if (!jwtUtil.validarToken(token) || !jwtUtil.isTokenDeUsuario(token)) {
            return;
        }

        String email = jwtUtil.extrairEmail(token);
        String role = jwtUtil.extrairRole(token);

        if (email == null || role == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("id", jwtUtil.extrairId(token));
        detalhes.put("nome", jwtUtil.extrairNome(token));
        detalhes.put("role", role);
        detalhes.put("web", new WebAuthenticationDetailsSource().buildDetails(request));
        authentication.setDetails(detalhes);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
