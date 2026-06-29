package com.clinica.servico_autenticacao.security.strategy;

import org.springframework.security.core.Authentication;

/**
 * Contrato para estratégias de autenticação baseadas em tipo de token JWT.
 *
 * Cada implementação é responsável por:
 * - Declarar qual tipo de token suporta
 * - Montar o objeto Authentication adequado para aquele tipo
 *
 * Novas estratégias podem ser adicionadas sem modificar o JwtFilter
 * (Open/Closed Principle).
 */
public interface TokenAuthenticationStrategy {

    /**
     * Indica se esta estratégia é capaz de processar o tipo informado.
     *
     * @param type valor da claim "type" extraída do token JWT
     * @return true se esta estratégia deve processar o token
     */
    boolean suporta(String type);

    /**
     * Constrói o objeto Authentication a partir do token JWT.
     * Só é chamado se suporta() retornar true.
     *
     * @param token JWT já validado (assinatura e expiração verificadas)
     * @return Authentication pronto para ser colocado no SecurityContextHolder
     */
    Authentication autenticar(String token);
}