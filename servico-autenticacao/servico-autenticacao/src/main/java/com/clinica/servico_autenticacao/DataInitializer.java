package com.clinica.servico_autenticacao;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Usuario(null, "Gerente", "gerente@clinica.com", passwordEncoder.encode("123456"), Role.GERENTE));
            usuarioRepository.save(new Usuario(null, "Atendente", "atendente@clinica.com", passwordEncoder.encode("123456"), Role.ATENDENTE));
            usuarioRepository.save(new Usuario(null, "Dentista", "dentista@clinica.com", passwordEncoder.encode("123456"), Role.DENTISTA));
            usuarioRepository.save(new Usuario(null, "Auxiliar", "auxiliar@clinica.com", passwordEncoder.encode("123456"), Role.AUXILIAR));
            System.out.println("Usuarios de teste criados!");
        }
    }
}
