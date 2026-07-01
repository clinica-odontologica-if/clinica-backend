package com.clinica.servico_financeiro.controller;

import com.clinica.servico_financeiro.dto.RelatorioFinanceiroResponse;
import com.clinica.servico_financeiro.service.RelatorioFinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioFinanceiroController {

    private final RelatorioFinanceiroService relatorioFinanceiroService;

    @GetMapping("/financeiro")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<RelatorioFinanceiroResponse> gerar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(relatorioFinanceiroService.gerar(dataInicio, dataFim));
    }
}