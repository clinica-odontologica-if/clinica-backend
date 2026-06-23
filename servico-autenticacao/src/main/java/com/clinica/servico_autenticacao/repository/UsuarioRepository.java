package com.clinica.servico_autenticacao.repository;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByRole(Role role);
}