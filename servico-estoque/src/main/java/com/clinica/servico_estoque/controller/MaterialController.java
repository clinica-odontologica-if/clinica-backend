package com.clinica.servico_estoque.controller;

import com.clinica.servico_estoque.dto.MaterialRequest;
import com.clinica.servico_estoque.dto.MaterialResponse;
import com.clinica.servico_estoque.service.MaterialService;
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

@RestController
@RequestMapping("/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA', 'AUXILIAR')")
    public ResponseEntity<List<MaterialResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean baixoEstoque,
            @RequestParam(required = false) Boolean ativo
    ) {
        return ResponseEntity.ok(materialService.listar(busca, categoria, baixoEstoque, ativo));
    }

    @GetMapping("/alertas/baixo-estoque")
    @PreAuthorize("hasAnyRole('GERENTE', 'AUXILIAR')")
    public ResponseEntity<List<MaterialResponse>> listarBaixoEstoque() {
        return ResponseEntity.ok(materialService.listarBaixoEstoque());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE', 'DENTISTA', 'AUXILIAR')")
    public ResponseEntity<MaterialResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'AUXILIAR')")
    public ResponseEntity<MaterialResponse> cadastrar(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.cadastrar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'AUXILIAR')")
    public ResponseEntity<MaterialResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody MaterialRequest request
    ) {
        return ResponseEntity.ok(materialService.atualizar(id, request));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        materialService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
