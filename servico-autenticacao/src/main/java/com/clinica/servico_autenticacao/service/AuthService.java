package com.clinica.servico_autenticacao.service;

import com.clinica.servico_autenticacao.dto.SetupRequest;
import com.clinica.servico_autenticacao.dto.UsuarioInternoRequest;
import com.clinica.servico_autenticacao.dto.UsuarioResponse;
import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario autenticar(String email, String senha) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            throw new RuntimeException("Usuario nao encontrado");
        }
        if (!usuario.get().isAtivo()) {
            throw new RuntimeException("Usuario inativo");
        }
        if (!passwordEncoder.matches(senha, usuario.get().getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }
        return usuario.get();
    }

    /**
     * Cadastra o primeiro Gerente do sistema.
     * Só é permitido enquanto não existir nenhum usuário com role GERENTE.
     * Após o primeiro cadastro, a rota permanece bloqueada por esta verificação.
     */
    public UsuarioResponse setup(SetupRequest request) {
        boolean gerenteExistente = usuarioRepository.existsByRole(Role.GERENTE);

        if (gerenteExistente) {
            log.warn("Tentativa de acesso ao /auth/setup bloqueada — gerente já cadastrado.");
            throw new SetupJaRealizadoException("Setup já foi realizado. Esta rota não está mais disponível.");
        }

        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("O campo nome é obrigatório.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("O campo email é obrigatório.");
        }
        if (request.getSenha() == null || request.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 6 caracteres.");
        }

        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }

        Usuario gerente = new Usuario(
                null,
                request.getNome(),
                request.getEmail(),
                passwordEncoder.encode(request.getSenha()),
                Role.GERENTE,
                true
        );

        Usuario salvo = usuarioRepository.save(gerente);
        log.info("Primeiro gerente criado com sucesso: {}", salvo.getEmail());

        return new UsuarioResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    public UsuarioResponse criarUsuarioInterno(UsuarioInternoRequest request) {
        validarUsuarioInterno(request);

        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ja existe um usuario com este email.");
        }

        Usuario usuario = new Usuario(
                null,
                request.getNome().trim(),
                request.getEmail().trim(),
                passwordEncoder.encode(request.getSenha()),
                request.getRole(),
                true
        );

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuario interno criado com sucesso: {}", salvo.getEmail());

        return new UsuarioResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    private void validarUsuarioInterno(UsuarioInternoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados do usuario sao obrigatorios.");
        }
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("O campo nome e obrigatorio.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("O campo email e obrigatorio.");
        }
        if (request.getSenha() == null || request.getSenha().isBlank()) {
            throw new IllegalArgumentException("O campo senha e obrigatorio.");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("O campo role e obrigatorio.");
        }
    }

    public void inativarUsuarioInterno(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O campo email e obrigatorio.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario interno inativado: {}", usuario.getEmail());
    }

    // Exceção interna para representar o estado "setup já feito"
    public static class SetupJaRealizadoException extends RuntimeException {
        public SetupJaRealizadoException(String mensagem) {
            super(mensagem);
        }
    }
}
