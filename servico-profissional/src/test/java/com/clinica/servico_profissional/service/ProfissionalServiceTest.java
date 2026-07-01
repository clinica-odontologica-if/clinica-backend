package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.ProfissionalRequest;
import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.dto.UsuarioInternoRequest;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.model.Role;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfissionalService")
class ProfissionalServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private AutenticacaoClientService autenticacaoClientService;

    @InjectMocks
    private ProfissionalService profissionalService;

    @Test
    @DisplayName("deve listar profissionais ativos aplicando filtros")
    void deveListarProfissionaisAtivosAplicandoFiltros() {
        Profissional profissional = new Profissional(
                1L,
                "Ana Souza",
                "ana@clinica.com",
                "CRO-MG123",
                "Ortodontia",
                Role.DENTISTA,
                true,
                null
        );

        when(profissionalRepository.buscarAtivosComFiltros("ana", "orto", Role.DENTISTA))
                .thenReturn(List.of(profissional));

        List<ProfissionalResponse> response = profissionalService.listarAtivos(
                " Ana ",
                " Orto ",
                Role.DENTISTA
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getNome()).isEqualTo("Ana Souza");
        verify(profissionalRepository).buscarAtivosComFiltros("ana", "orto", Role.DENTISTA);
    }

    @Test
    @DisplayName("deve buscar profissional ativo por email")
    void deveBuscarProfissionalAtivoPorEmail() {
        Profissional profissional = new Profissional(
                7L,
                "Dra Maria",
                "maria@clinica.com",
                "CRO-MG777",
                "Endodontia",
                Role.DENTISTA,
                true,
                null
        );

        when(profissionalRepository.findByEmail("maria@clinica.com")).thenReturn(Optional.of(profissional));

        ProfissionalResponse response = profissionalService.buscarPorEmail(" MARIA@CLINICA.COM ");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("maria@clinica.com");
    }

    @Test
    @DisplayName("deve cadastrar dentista criando usuario interno antes de salvar")
    void deveCadastrarDentistaCriandoUsuarioInterno() {
        ProfissionalRequest request = new ProfissionalRequest(
                "Joao Silva",
                "JOAO@CLINICA.COM",
                "CRO-SP12345",
                "Ortodontia",
                Role.DENTISTA
        );

        when(profissionalRepository.existsByEmail("joao@clinica.com")).thenReturn(false);
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(invocation -> {
            Profissional profissional = invocation.getArgument(0);
            profissional.setId(10L);
            return profissional;
        });

        ProfissionalResponse response = profissionalService.cadastrar(request);

        ArgumentCaptor<UsuarioInternoRequest> usuarioCaptor = ArgumentCaptor.forClass(UsuarioInternoRequest.class);
        ArgumentCaptor<Profissional> profissionalCaptor = ArgumentCaptor.forClass(Profissional.class);

        verify(autenticacaoClientService).criarUsuarioInterno(usuarioCaptor.capture());
        verify(profissionalRepository).save(profissionalCaptor.capture());

        UsuarioInternoRequest usuarioInterno = usuarioCaptor.getValue();
        assertThat(usuarioInterno.getEmail()).isEqualTo("joao@clinica.com");
        assertThat(usuarioInterno.getSenha()).isEqualTo("Joao@" + Year.now().getValue());
        assertThat(usuarioInterno.getRole()).isEqualTo(Role.DENTISTA);

        Profissional salvo = profissionalCaptor.getValue();
        assertThat(salvo.getCro()).isEqualTo("CRO-SP12345");
        assertThat(salvo.getEspecialidade()).isEqualTo("Ortodontia");
        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deve exigir cro e especialidade para dentista")
    void deveExigirCroEEspecialidadeParaDentista() {
        ProfissionalRequest request = new ProfissionalRequest(
                "Maria",
                "maria@clinica.com",
                "",
                "",
                Role.DENTISTA
        );

        assertThatThrownBy(() -> profissionalService.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("cro");

        verify(autenticacaoClientService, never()).criarUsuarioInterno(any());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve bloquear alteracao de email para manter consistencia com autenticacao")
    void deveBloquearAlteracaoDeEmail() {
        Profissional profissional = new Profissional(
                1L,
                "Carlos",
                "carlos@clinica.com",
                null,
                null,
                Role.AUXILIAR,
                true,
                null
        );

        ProfissionalRequest request = new ProfissionalRequest(
                "Carlos",
                "novo@clinica.com",
                null,
                null,
                Role.AUXILIAR
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> profissionalService.atualizar(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Email");

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve inativar profissional e usuario de autenticacao")
    void deveInativarProfissionalEUsuarioDeAutenticacao() {
        Profissional profissional = new Profissional(
                1L,
                "Carlos",
                "carlos@clinica.com",
                null,
                null,
                Role.AUXILIAR,
                true,
                null
        );

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional));

        profissionalService.inativar(1L);

        assertThat(profissional.isAtivo()).isFalse();
        verify(profissionalRepository).save(profissional);
        verify(autenticacaoClientService).inativarUsuarioInterno("carlos@clinica.com");
    }
}
