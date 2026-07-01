package com.clinica.servico_atendimento.client;

import com.clinica.servico_atendimento.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PacienteClient")
class PacienteClientTest {

    @Test
    @DisplayName("deve buscar paciente por id e repassar authorization")
    void deveBuscarPacientePorIdERepassarAuthorization() {
        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/pacientes/12");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");

            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "id": 12,
                              "nome": "Maria Silva",
                              "cpf": "12345678900",
                              "email": "maria@clinica.com",
                              "telefone": "31999999999",
                              "ativo": true
                            }
                            """)
                    .build());
        };

        PacienteClient client = new PacienteClient(WebClient.builder().exchangeFunction(exchangeFunction), "http://pacientes/");

        PacienteClientResponse response = client.buscarPorId(12L, "Bearer token");

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.nome()).isEqualTo("Maria Silva");
        assertThat(response.ativo()).isTrue();
    }

    @Test
    @DisplayName("deve converter 404 em recurso nao encontrado")
    void deveConverter404EmRecursoNaoEncontrado() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        PacienteClient client = new PacienteClient(WebClient.builder().exchangeFunction(exchangeFunction), "http://pacientes");

        assertThatThrownBy(() -> client.buscarPorId(99L, "Bearer token"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Paciente com id 99 nao encontrado");
    }
}
