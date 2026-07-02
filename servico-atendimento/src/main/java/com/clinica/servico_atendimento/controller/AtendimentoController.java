package com.clinica.servico_atendimento.controller;

import com.clinica.servico_atendimento.dto.AtendimentoRequest;
import com.clinica.servico_atendimento.dto.AtendimentoResponse;
import com.clinica.servico_atendimento.dto.RealizacaoAtendimentoRequest;
import com.clinica.servico_atendimento.dto.StatusAtendimentoRequest;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import com.clinica.servico_atendimento.service.AtendimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequiredArgsConstructor
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<AtendimentoResponse> cadastrar(
            @Valid @RequestBody AtendimentoRequest dto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atendimentoService.cadastrar(dto, authorizationHeader));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA')")
    public ResponseEntity<List<AtendimentoResponse>> listar(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) StatusAtendimento status,
            @RequestParam(required = false) String busca,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(atendimentoService.listar(
                pacienteId,
                profissionalId,
                data,
                dataInicio,
                dataFim,
                status,
                busca,
                authentication,
                authorizationHeader
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA')")
    public ResponseEntity<AtendimentoResponse> buscarPorId(
            @PathVariable Long id,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id, authentication, authorizationHeader));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA')")
    public ResponseEntity<AtendimentoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusAtendimentoRequest dto,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(atendimentoService.atualizarStatus(id, dto, authentication, authorizationHeader));
    }

    @PatchMapping("/{id}/realizar")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA')")
    public ResponseEntity<AtendimentoResponse> realizar(
            @PathVariable Long id,
            @Valid @RequestBody RealizacaoAtendimentoRequest dto,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(atendimentoService.realizar(id, dto, authentication, authorizationHeader));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA')")
    public ResponseEntity<AtendimentoResponse> cancelar(
            @PathVariable Long id,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(atendimentoService.cancelar(id, authentication, authorizationHeader));
    }
}
