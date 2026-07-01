package com.clinica.servico_financeiro.client;

import com.clinica.servico_financeiro.exception.RecursoNaoEncontradoException;
import com.clinica.servico_financeiro.exception.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AtendimentoClient {

    private final RestClient restClient;

    public AtendimentoClient(@Value("${servico.atendimento.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public AtendimentoClientResponse buscarPorId(Long atendimentoId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/atendimentos/{id}", atendimentoId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(AtendimentoClientResponse.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RecursoNaoEncontradoException("Atendimento nao encontrado");
        } catch (RestClientException exception) {
            throw new ServicoIndisponivelException("Servico de atendimento indisponivel");
        }
    }
}