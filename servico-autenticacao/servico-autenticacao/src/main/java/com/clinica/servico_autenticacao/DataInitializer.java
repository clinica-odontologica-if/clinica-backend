package com.clinica.servico_autenticacao;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.senha.gerente}")
    private String senhaGerente;

    @Value("${admin.senha.atendente}")
    private String senhaAtendente;

    @Value("${admin.senha.dentista}")
    private String senhaDentista;

    @Value("${admin.senha.auxiliar}")
    private String senhaAuxiliar;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Usuario(null, "Gerente", "gerente@clinica.com", passwordEncoder.encode(senhaGerente), Role.GERENTE));
            usuarioRepository.save(new Usuario(null, "Atendente", "atendente@clinica.com", passwordEncoder.encode(senhaAtendente), Role.ATENDENTE));
            usuarioRepository.save(new Usuario(null, "Dentista", "dentista@clinica.com", passwordEncoder.encode(senhaDentista), Role.DENTISTA));
            usuarioRepository.save(new Usuario(null, "Auxiliar", "auxiliar@clinica.com", passwordEncoder.encode(senhaAuxiliar), Role.AUXILIAR));
        }
    }
}
