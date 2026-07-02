package com.clinica.servico_financeiro.controller;

import com.clinica.servico_financeiro.dto.ReceitaRequest;
import com.clinica.servico_financeiro.dto.ReceitaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.service.ReceitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/receitas")
@RequiredArgsConstructor
public class ReceitaController {

    private final ReceitaService receitaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<ReceitaResponse> cadastrar(
            @Valid @RequestBody ReceitaRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receitaService.cadastrar(request, authorizationHeader));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<List<ReceitaResponse>> listar(
            @RequestParam(required = false) Long atendimentoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) StatusFinanceiro status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(receitaService.listar(atendimentoId, pacienteId, profissionalId, status, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<ReceitaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(receitaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<ReceitaResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusFinanceiroRequest request
    ) {
        return ResponseEntity.ok(receitaService.atualizarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        receitaService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}