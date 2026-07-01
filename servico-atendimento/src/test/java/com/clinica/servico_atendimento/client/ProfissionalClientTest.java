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

@DisplayName("ProfissionalClient")
class ProfissionalClientTest {

    @Test
    @DisplayName("deve buscar profissional por id")
    void deveBuscarProfissionalPorId() {
        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/profissionais/5");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");

            return Mono.just(criarRespostaOk());
        };

        ProfissionalClient client = new ProfissionalClient(WebClient.builder().exchangeFunction(exchangeFunction), "http://profissionais/");

        ProfissionalClientResponse response = client.buscarPorId(5L, "Bearer token");

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nome()).isEqualTo("Dr Joao");
        assertThat(response.especialidade()).isEqualTo("Ortodontia");
    }

    @Test
    @DisplayName("deve buscar perfil do profissional autenticado")
    void deveBuscarPerfilDoProfissionalAutenticado() {
        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/profissionais/me");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");

            return Mono.just(criarRespostaOk());
        };

        ProfissionalClient client = new ProfissionalClient(WebClient.builder().exchangeFunction(exchangeFunction), "http://profissionais");

        ProfissionalClientResponse response = client.buscarMeuPerfil("Bearer token");

        assertThat(response.email()).isEqualTo("joao@clinica.com");
        assertThat(response.role()).isEqualTo("DENTISTA");
    }

    @Test
    @DisplayName("deve converter 404 em recurso nao encontrado")
    void deveConverter404EmRecursoNaoEncontrado() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        ProfissionalClient client = new ProfissionalClient(WebClient.builder().exchangeFunction(exchangeFunction), "http://profissionais");

        assertThatThrownBy(() -> client.buscarPorId(99L, "Bearer token"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Profissional com id 99 nao encontrado");
    }

    private ClientResponse criarRespostaOk() {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "id": 5,
                          "nome": "Dr Joao",
                          "email": "joao@clinica.com",
                          "cro": "CRO-1234",
                          "especialidade": "Ortodontia",
                          "role": "DENTISTA",
                          "ativo": true
                        }
                        """)
                .build();
    }
}
