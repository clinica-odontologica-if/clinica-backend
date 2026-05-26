package com.clinica.servico_autenticacao.service;

import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        if (!passwordEncoder.matches(senha, usuario.get().getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }
        return usuario.get();
    }

}
