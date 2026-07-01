package com.clinica.servico_atendimento.client;

import com.clinica.servico_atendimento.exception.RecursoNaoEncontradoException;
import com.clinica.servico_atendimento.exception.RegraDeNegocioException;
import com.clinica.servico_atendimento.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProfissionalClient {

    private final WebClient webClient;

    public ProfissionalClient(
            WebClient.Builder webClientBuilder,
            @Value("${clinica.servico-profissional.url:http://localhost:8082}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(removerBarraFinal(baseUrl))
                .build();
    }

    public ProfissionalClientResponse buscarPorId(Long id, String authorizationHeader) {
        try {
            ProfissionalClientResponse profissional = webClient.get()
                    .uri("/profissionais/{id}", id)
                    .headers(headers -> adicionarAuthorization(headers, authorizationHeader))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> tratarErroCliente(response.statusCode(), id))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ServicoIndisponivelException(
                            "Servico de profissionais indisponivel"
                    )))
                    .bodyToMono(ProfissionalClientResponse.class)
                    .block();

            if (profissional == null) {
                throw new ServicoIndisponivelException("Servico de profissionais retornou resposta vazia");
            }

            return profissional;
        } catch (WebClientRequestException exception) {
            log.warn("Falha ao consultar servico-profissional: {}", exception.getMessage());
            throw new ServicoIndisponivelException("Nao foi possivel conectar ao servico de profissionais");
        }
    }

    public ProfissionalClientResponse buscarMeuPerfil(String authorizationHeader) {
        try {
            ProfissionalClientResponse profissional = webClient.get()
                    .uri("/profissionais/me")
                    .headers(headers -> adicionarAuthorization(headers, authorizationHeader))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new RegraDeNegocioException(
                            "Nao foi possivel identificar o profissional autenticado"
                    )))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ServicoIndisponivelException(
                            "Servico de profissionais indisponivel"
                    )))
                    .bodyToMono(ProfissionalClientResponse.class)
                    .block();

            if (profissional == null) {
                throw new ServicoIndisponivelException("Servico de profissionais retornou resposta vazia");
            }

            return profissional;
        } catch (WebClientRequestException exception) {
            log.warn("Falha ao consultar perfil no servico-profissional: {}", exception.getMessage());
            throw new ServicoIndisponivelException("Nao foi possivel conectar ao servico de profissionais");
        }
    }

    private Mono<? extends Throwable> tratarErroCliente(HttpStatusCode statusCode, Long id) {
        if (statusCode == HttpStatus.NOT_FOUND) {
            return Mono.error(new RecursoNaoEncontradoException("Profissional com id " + id + " nao encontrado"));
        }

        return Mono.error(new RegraDeNegocioException("Profissional nao pode ser validado pelo servico de profissionais"));
    }

    private void adicionarAuthorization(HttpHeaders headers, String authorizationHeader) {
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }

    private String removerBarraFinal(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
