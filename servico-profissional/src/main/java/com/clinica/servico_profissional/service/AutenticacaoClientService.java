package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.UsuarioInternoRequest;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
public class AutenticacaoClientService {

    private static final String NOME_SERVICO = "servico-profissional";

    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;
    private final String autenticacaoBaseUrl;

    public AutenticacaoClientService(WebClient.Builder webClientBuilder,
                                     JwtUtil jwtUtil,
                                     @Value("${SERVICO_AUTENTICACAO_URL:http://localhost:8081}") String autenticacaoBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.jwtUtil = jwtUtil;
        this.autenticacaoBaseUrl = autenticacaoBaseUrl.replaceAll("/+$", "");
    }

    public void criarUsuarioInterno(UsuarioInternoRequest request) {
        String tokenServico = jwtUtil.gerarTokenServico(NOME_SERVICO);

        try {
            webClientBuilder.build()
                    .post()
                    .uri(autenticacaoBaseUrl + "/auth/usuarios/interno")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServico)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Servico de autenticacao recusou criacao de usuario interno. Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RegraDeNegocioException(extrairMensagemErro(e));
        } catch (WebClientRequestException e) {
            log.warn("Falha de comunicacao com servico de autenticacao em {}: {}",
                    autenticacaoBaseUrl, e.getMessage());
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
        if (body != null && !body.isBlank()) {
            return "Falha ao criar usuario no servico de autenticacao. Status "
                    + e.getStatusCode().value() + ": " + body;
        }
        return "Falha ao criar usuario no servico de autenticacao. Status "
                + e.getStatusCode().value() + ".";
    }
}
