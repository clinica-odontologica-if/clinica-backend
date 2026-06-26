package com.clinica.servico_paciente.exception;

import com.clinica.servico_paciente.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
                new ErroResponse(404, "Recurso nao encontrado", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(400).body(
                new ErroResponse(400, "Dados invalidos", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(400).body(
                new ErroResponse(400, "Dados invalidos", mensagem, request.getRequestURI())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(403).body(
                new ErroResponse(403, "Acesso negado", "Seu perfil nao tem permissao para esta acao", request.getRequestURI())
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
