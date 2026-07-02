package com.clinica.servico_financeiro.exception;

public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String message) {
        super(message);
    }
}