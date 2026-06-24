package com.clinica.servico_profissional.exception;

import com.clinica.servico_profissional.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
                new ErroResponse(404, "Recurso não encontrado", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(400).body(
                new ErroResponse(400, "Dados inválidos", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleErroGenerico(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(
                new ErroResponse(500, "Erro interno", "Ocorreu um erro inesperado", request.getRequestURI())
        );
    }
}