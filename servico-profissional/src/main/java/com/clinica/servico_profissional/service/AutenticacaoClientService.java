package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.UsuarioInternoRequest;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class AutenticacaoClientService {

    private static final String NOME_SERVICO = "servico-profissional";
    private static final String USUARIO_INTERNO_URI = "http://servico-autenticacao/auth/usuarios/interno";

    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;

    public void criarUsuarioInterno(UsuarioInternoRequest request) {
        String tokenServico = jwtUtil.gerarTokenServico(NOME_SERVICO);

        try {
            webClientBuilder.build()
                    .post()
                    .uri(USUARIO_INTERNO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServico)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw new RegraDeNegocioException(extrairMensagemErro(e));
        } catch (WebClientRequestException e) {
            throw new RegraDeNegocioException("Nao foi possivel comunicar com o servico de autenticacao.");
        }
    }

    private String extrairMensagemErro(WebClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && body.contains("Ja existe um usuario com este email")) {
            return "Ja existe um usuario com este email.";
        }
        if (body != null && body.contains("mensagem")) {
            return body;
        }
        return "Falha ao criar usuario no servico de autenticacao.";
    }
}
