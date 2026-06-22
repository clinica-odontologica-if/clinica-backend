package com.clinica.servico_autenticacao;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")   // ← só executa quando spring.profiles.active=dev
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
            usuarioRepository.save(new Usuario(null, "Gerente",   "gerente@clinica.com",   passwordEncoder.encode(senhaGerente),   Role.GERENTE));
            usuarioRepository.save(new Usuario(null, "Atendente", "atendente@clinica.com", passwordEncoder.encode(senhaAtendente), Role.ATENDENTE));
            usuarioRepository.save(new Usuario(null, "Dentista",  "dentista@clinica.com",  passwordEncoder.encode(senhaDentista),  Role.DENTISTA));
            usuarioRepository.save(new Usuario(null, "Auxiliar",  "auxiliar@clinica.com",  passwordEncoder.encode(senhaAuxiliar),  Role.AUXILIAR));
            log.info("Usuarios de desenvolvimento criados com sucesso.");
        } else {
            log.info("Banco ja populado — seed ignorado.");
        }
    }
}