package com.clinica.servico_atendimento.service;

import com.clinica.servico_atendimento.client.PacienteClient;
import com.clinica.servico_atendimento.client.PacienteClientResponse;
import com.clinica.servico_atendimento.client.ProfissionalClient;
import com.clinica.servico_atendimento.client.ProfissionalClientResponse;
import com.clinica.servico_atendimento.dto.AtendimentoRequest;
import com.clinica.servico_atendimento.dto.AtendimentoResponse;
import com.clinica.servico_atendimento.dto.StatusAtendimentoRequest;
import com.clinica.servico_atendimento.exception.RegraDeNegocioException;
import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import com.clinica.servico_atendimento.repository.AtendimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtendimentoService")
class AtendimentoServiceTest {

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private PacienteClient pacienteClient;

    @Mock
    private ProfissionalClient profissionalClient;

    @InjectMocks
    private AtendimentoService atendimentoService;

    @Test
    @DisplayName("deve cadastrar atendimento validando paciente, profissional e conflito")
    void deveCadastrarAtendimento() {
        AtendimentoRequest request = requestPadrao();
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndHoraAtendimentoAndStatusInAndAtivoTrue(
                eq(2L), eq(request.data()), eq(request.hora()), any()
        )).thenReturn(false);
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> {
            Atendimento atendimento = invocation.getArgument(0);
            atendimento.setId(10L);
            return atendimento;
        });

        AtendimentoResponse response = atendimentoService.cadastrar(request, "Bearer token");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.pacienteNome()).isEqualTo("Maria Silva");
        assertThat(response.profissionalNome()).isEqualTo("Dr Joao");
        assertThat(response.status()).isEqualTo(StatusAtendimento.AGENDADO);

        ArgumentCaptor<Atendimento> captor = ArgumentCaptor.forClass(Atendimento.class);
        verify(atendimentoRepository).save(captor.capture());
        assertThat(captor.getValue().getProfissionalEmail()).isEqualTo("joao@clinica.com");
    }

    @Test
    @DisplayName("deve bloquear cadastro quando existe conflito de horario")
    void deveBloquearConflitoDeHorario() {
        AtendimentoRequest request = requestPadrao();
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndHoraAtendimentoAndStatusInAndAtivoTrue(
                eq(2L), eq(request.data()), eq(request.hora()), any()
        )).thenReturn(true);

        assertThatThrownBy(() -> atendimentoService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Profissional ja possui atendimento agendado nesse horario");
    }

    @Test
    @DisplayName("deve listar apenas atendimentos do dentista autenticado")
    void deveListarApenasAtendimentosDoDentista() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(profissionalClient.buscarMeuPerfil("Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.buscarAtivosComFiltros(null, 2L, null, null)).thenReturn(List.of(atendimento));

        List<AtendimentoResponse> response = atendimentoService.listar(
                null,
                99L,
                null,
                null,
                autenticacaoDentista(),
                "Bearer token"
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().profissionalId()).isEqualTo(2L);
        verify(atendimentoRepository).buscarAtivosComFiltros(null, 2L, null, null);
    }

    @Test
    @DisplayName("deve permitir gerente listar com filtros informados")
    void devePermitirGerenteListarComFiltros() {
        LocalDate data = LocalDate.now().plusDays(1);
        when(atendimentoRepository.buscarAtivosComFiltros(1L, 2L, data, StatusAtendimento.AGENDADO))
                .thenReturn(List.of(atendimentoSalvo(2L)));

        List<AtendimentoResponse> response = atendimentoService.listar(
                1L,
                2L,
                data,
                StatusAtendimento.AGENDADO,
                autenticacaoGerente(),
                "Bearer token"
        );

        assertThat(response).hasSize(1);
        verify(atendimentoRepository).buscarAtivosComFiltros(1L, 2L, data, StatusAtendimento.AGENDADO);
    }

    @Test
    @DisplayName("deve impedir dentista de alterar atendimento de outro profissional")
    void deveImpedirDentistaDeAlterarAtendimentoDeOutroProfissional() {
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimentoSalvo(99L)));
        when(profissionalClient.buscarMeuPerfil("Bearer token")).thenReturn(dentistaAtivo(2L));

        assertThatThrownBy(() -> atendimentoService.atualizarStatus(
                10L,
                new StatusAtendimentoRequest(StatusAtendimento.REALIZADO),
                autenticacaoDentista(),
                "Bearer token"
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Dentista pode alterar apenas os proprios atendimentos");
    }

    @Test
    @DisplayName("deve atualizar status e marcar realizado em")
    void deveAtualizarStatusEMarcarRealizadoEm() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(atendimento)).thenReturn(atendimento);

        AtendimentoResponse response = atendimentoService.atualizarStatus(
                10L,
                new StatusAtendimentoRequest(StatusAtendimento.REALIZADO),
                autenticacaoGerente(),
                "Bearer token"
        );

        assertThat(response.status()).isEqualTo(StatusAtendimento.REALIZADO);
        assertThat(response.realizadoEm()).isNotNull();
    }

    private AtendimentoRequest requestPadrao() {
        return new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                "Consulta inicial"
        );
    }

    private PacienteClientResponse pacienteAtivo() {
        return new PacienteClientResponse(1L, "Maria Silva", "12345678900", "maria@clinica.com", "31999999999", true);
    }

    private ProfissionalClientResponse dentistaAtivo(Long id) {
        return new ProfissionalClientResponse(id, "Dr Joao", "joao@clinica.com", "CRO-1234", "Ortodontia", "DENTISTA", true);
    }

    private Atendimento atendimentoSalvo(Long profissionalId) {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(10L);
        atendimento.setPacienteId(1L);
        atendimento.setPacienteNome("Maria Silva");
        atendimento.setProfissionalId(profissionalId);
        atendimento.setProfissionalNome("Dr Joao");
        atendimento.setProfissionalEmail("joao@clinica.com");
        atendimento.setDataAtendimento(LocalDate.now().plusDays(1));
        atendimento.setHoraAtendimento(LocalTime.of(9, 0));
        atendimento.setStatus(StatusAtendimento.AGENDADO);
        atendimento.setAtivo(true);
        return atendimento;
    }

    private UsernamePasswordAuthenticationToken autenticacaoDentista() {
        return new UsernamePasswordAuthenticationToken(
                "dentista@clinica.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DENTISTA"))
        );
    }

    private UsernamePasswordAuthenticationToken autenticacaoGerente() {
        return new UsernamePasswordAuthenticationToken(
                "gerente@clinica.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))
        );
    }
}
