package com.clinica.servico_financeiro.controller;

import com.clinica.servico_financeiro.dto.DespesaRequest;
import com.clinica.servico_financeiro.dto.DespesaResponse;
import com.clinica.servico_financeiro.dto.StatusFinanceiroRequest;
import com.clinica.servico_financeiro.model.CategoriaDespesa;
import com.clinica.servico_financeiro.model.StatusFinanceiro;
import com.clinica.servico_financeiro.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/despesas")
@RequiredArgsConstructor
public class DespesaController {

    private final DespesaService despesaService;

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<DespesaResponse> cadastrar(@Valid @RequestBody DespesaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(despesaService.cadastrar(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<List<DespesaResponse>> listar(
            @RequestParam(required = false) CategoriaDespesa categoria,
            @RequestParam(required = false) StatusFinanceiro status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(despesaService.listar(categoria, status, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<DespesaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<DespesaResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusFinanceiroRequest request
    ) {
        return ResponseEntity.ok(despesaService.atualizarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        despesaService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}