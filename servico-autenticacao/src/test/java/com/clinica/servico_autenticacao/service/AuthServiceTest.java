package com.clinica.servico_autenticacao.service;

import com.clinica.servico_autenticacao.model.Role;
import com.clinica.servico_autenticacao.model.Usuario;
import com.clinica.servico_autenticacao.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — testes unitários")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioGerente;

    @BeforeEach
    void setUp() {
        usuarioGerente = new Usuario(
                1L,
                "Gerente",
                "gerente@clinica.com",
                "$2a$10$hashFakeParaTeste",
                Role.GERENTE,
                true
        );
    }

    @Test
    @DisplayName("deve autenticar com sucesso quando email e senha estão corretos")
    void deveAutenticarComSucesso() {
        when(usuarioRepository.findByEmail("gerente@clinica.com"))
                .thenReturn(Optional.of(usuarioGerente));
        when(passwordEncoder.matches("senha123", usuarioGerente.getSenha()))
                .thenReturn(true);

        Usuario resultado = authService.autenticar("gerente@clinica.com", "senha123");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("gerente@clinica.com");
        assertThat(resultado.getRole()).isEqualTo(Role.GERENTE);
    }

    @Test
    @DisplayName("deve lançar exceção quando email não está cadastrado")
    void deveLancarExcecaoQuandoEmailNaoEncontrado() {
        when(usuarioRepository.findByEmail("inexistente@clinica.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.autenticar("inexistente@clinica.com", "qualquerSenha")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario nao encontrado");
    }

    @Test
    @DisplayName("deve lançar exceção quando senha está incorreta")
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        when(usuarioRepository.findByEmail("gerente@clinica.com"))
                .thenReturn(Optional.of(usuarioGerente));
        when(passwordEncoder.matches("senhaErrada", usuarioGerente.getSenha()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authService.autenticar("gerente@clinica.com", "senhaErrada")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Senha incorreta");
    }

    @Test
    @DisplayName("deve bloquear autenticacao quando usuario esta inativo")
    void deveBloquearAutenticacaoQuandoUsuarioInativo() {
        usuarioGerente.setAtivo(false);
        when(usuarioRepository.findByEmail("gerente@clinica.com"))
                .thenReturn(Optional.of(usuarioGerente));

        assertThatThrownBy(() ->
                authService.autenticar("gerente@clinica.com", "senha123")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario inativo");
    }
}
