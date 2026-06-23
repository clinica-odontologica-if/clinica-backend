package com.clinica.servico_autenticacao.controller;

import com.clinica.servico_autenticacao.dto.SetupRequest;
import com.clinica.servico_autenticacao.dto.UsuarioResponse;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.security.JwtUtil;
import com.clinica.servico_autenticacao.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String senha = body.get("senha");

            Usuario usuario = authService.autenticar(email, senha);
            String token = jwtUtil.gerarToken(usuario);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Rota pública de setup inicial do sistema.
     * Cria o primeiro gerente quando nenhum ainda existe.
     * Após o primeiro cadastro, retorna 403 em todas as chamadas subsequentes.
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestBody SetupRequest request) {
        try {
            UsuarioResponse response = authService.setup(request);
            return ResponseEntity.status(201).body(response);
        } catch (AuthService.SetupJaRealizadoException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", 403,
                    "erro", "Acesso negado",
                    "mensagem", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "erro", "Dados inválidos",
                    "mensagem", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}