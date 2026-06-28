package com.clinica.servico_paciente.controller;

import com.clinica.servico_paciente.dto.PacienteRequest;
import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "servico-paciente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA', 'AUXILIAR')")
    public ResponseEntity<List<PacienteResponse>> listar(@RequestParam(required = false) String busca,
                                                         @RequestParam(required = false) String cpf) {
        return ResponseEntity.ok(pacienteService.listarAtivos(busca, cpf));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA', 'AUXILIAR')")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<PacienteResponse> cadastrar(@Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.cadastrar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.ok(pacienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        pacienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
