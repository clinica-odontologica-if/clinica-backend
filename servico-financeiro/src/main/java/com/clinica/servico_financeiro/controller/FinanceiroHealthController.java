package com.clinica.servico_financeiro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/financeiro")
public class FinanceiroHealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "servico-financeiro"
        );
    }
}
