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
public class PacienteClient {

    private final WebClient webClient;

    public PacienteClient(
            WebClient.Builder webClientBuilder,
            @Value("${clinica.servico-paciente.url:http://localhost:8083}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(removerBarraFinal(baseUrl))
                .build();
    }

    public PacienteClientResponse buscarPorId(Long id, String authorizationHeader) {
        try {
            PacienteClientResponse paciente = webClient.get()
                    .uri("/pacientes/{id}", id)
                    .headers(headers -> adicionarAuthorization(headers, authorizationHeader))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> tratarErroCliente(response.statusCode(), id))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ServicoIndisponivelException(
                            "Servico de pacientes indisponivel"
                    )))
                    .bodyToMono(PacienteClientResponse.class)
                    .block();

            if (paciente == null) {
                throw new ServicoIndisponivelException("Servico de pacientes retornou resposta vazia");
            }

            return paciente;
        } catch (WebClientRequestException exception) {
            log.warn("Falha ao consultar servico-paciente: {}", exception.getMessage());
            throw new ServicoIndisponivelException("Nao foi possivel conectar ao servico de pacientes");
        }
    }

    private Mono<? extends Throwable> tratarErroCliente(HttpStatusCode statusCode, Long id) {
        if (statusCode == HttpStatus.NOT_FOUND) {
            return Mono.error(new RecursoNaoEncontradoException("Paciente com id " + id + " nao encontrado"));
        }

        return Mono.error(new RegraDeNegocioException("Paciente nao pode ser validado pelo servico de pacientes"));
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
