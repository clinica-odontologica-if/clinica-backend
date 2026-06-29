package com.clinica.servico_atendimento.exception;

import com.clinica.servico_atendimento.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.NOT_FOUND, "Recurso nao encontrado", exception.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(
            RegraDeNegocioException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.BAD_REQUEST, "Regra de negocio violada", exception.getMessage(), request);
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> tratarServicoIndisponivel(
            ServicoIndisponivelException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.SERVICE_UNAVAILABLE, "Servico indisponivel", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInterno(Exception exception, HttpServletRequest request) {
        return criarResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Nao foi possivel processar a requisicao",
                request
        );
    }

    private ResponseEntity<ErroResponse> criarResposta(
            HttpStatus status,
            String erro,
            String mensagem,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(new ErroResponse(status.value(), erro, mensagem, request.getRequestURI()));
    }
}
