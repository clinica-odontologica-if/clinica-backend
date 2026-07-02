package com.clinica.servico_atendimento.service;

import com.clinica.servico_atendimento.client.PacienteClient;
import com.clinica.servico_atendimento.client.PacienteClientResponse;
import com.clinica.servico_atendimento.client.ProfissionalClient;
import com.clinica.servico_atendimento.client.ProfissionalClientResponse;
import com.clinica.servico_atendimento.dto.AtendimentoRequest;
import com.clinica.servico_atendimento.dto.AtendimentoResponse;
import com.clinica.servico_atendimento.dto.RealizacaoAtendimentoRequest;
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

import java.math.BigDecimal;
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
        when(atendimentoRepository.buscarAtendimentosQueOcupamAgenda(eq(1L), eq(2L), eq(request.data()), any()))
                .thenReturn(List.of());
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
        assertThat(response.duracaoMinutos()).isEqualTo(45);

        ArgumentCaptor<Atendimento> captor = ArgumentCaptor.forClass(Atendimento.class);
        verify(atendimentoRepository).save(captor.capture());
        assertThat(captor.getValue().getProfissionalEmail()).isEqualTo("joao@clinica.com");
        assertThat(captor.getValue().getDuracaoMinutos()).isEqualTo(45);
    }

    @Test
    @DisplayName("deve usar duracao padrao quando nao informada")
    void deveUsarDuracaoPadraoQuandoNaoInformada() {
        AtendimentoRequest request = new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                null,
                "Consulta inicial"
        );
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.buscarAtendimentosQueOcupamAgenda(eq(1L), eq(2L), eq(request.data()), any()))
                .thenReturn(List.of());
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtendimentoResponse response = atendimentoService.cadastrar(request, "Bearer token");

        assertThat(response.duracaoMinutos()).isEqualTo(60);
    }

    @Test
    @DisplayName("deve bloquear cadastro quando dentista ja possui atendimento no intervalo")
    void deveBloquearConflitoDeHorarioDoDentista() {
        AtendimentoRequest request = new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 30),
                30,
                "Retorno"
        );
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.buscarAtendimentosQueOcupamAgenda(eq(1L), eq(2L), eq(request.data()), any()))
                .thenReturn(List.of(atendimentoSalvo(2L, 99L, LocalTime.of(9, 0), 60)));

        assertThatThrownBy(() -> atendimentoService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Profissional ja possui atendimento nesse intervalo de horario");
    }

    @Test
    @DisplayName("deve bloquear cadastro quando paciente ja possui atendimento no intervalo")
    void deveBloquearConflitoDeHorarioDoPaciente() {
        AtendimentoRequest request = new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 15),
                30,
                "Retorno"
        );
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.buscarAtendimentosQueOcupamAgenda(eq(1L), eq(2L), eq(request.data()), any()))
                .thenReturn(List.of(atendimentoSalvo(99L, 1L, LocalTime.of(10, 0), 60)));

        assertThatThrownBy(() -> atendimentoService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Paciente ja possui atendimento nesse intervalo de horario");
    }

    @Test
    @DisplayName("deve impedir atendimento que ultrapassa o dia")
    void deveImpedirAtendimentoQueUltrapassaODia() {
        AtendimentoRequest request = new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(23, 30),
                60,
                "Plantao"
        );
        when(pacienteClient.buscarPorId(1L, "Bearer token")).thenReturn(pacienteAtivo());
        when(profissionalClient.buscarPorId(2L, "Bearer token")).thenReturn(dentistaAtivo(2L));

        assertThatThrownBy(() -> atendimentoService.cadastrar(request, "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Horario e duracao do atendimento devem terminar no mesmo dia");
    }

    @Test
    @DisplayName("deve listar apenas atendimentos do dentista autenticado")
    void deveListarApenasAtendimentosDoDentista() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(profissionalClient.buscarMeuPerfil("Bearer token")).thenReturn(dentistaAtivo(2L));
        when(atendimentoRepository.buscarAtivosComFiltros(null, 2L, null, null, null, null, null)).thenReturn(List.of(atendimento));

        List<AtendimentoResponse> response = atendimentoService.listar(
                null,
                99L,
                null,
                null,
                null,
                null,
                null,
                autenticacaoDentista(),
                "Bearer token"
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().profissionalId()).isEqualTo(2L);
        verify(atendimentoRepository).buscarAtivosComFiltros(null, 2L, null, null, null, null, null);
    }

    @Test
    @DisplayName("deve permitir gerente listar com filtros informados")
    void devePermitirGerenteListarComFiltros() {
        LocalDate data = LocalDate.now().plusDays(1);
        when(atendimentoRepository.buscarAtivosComFiltros(1L, 2L, data, null, null, StatusAtendimento.AGENDADO, null))
                .thenReturn(List.of(atendimentoSalvo(2L)));

        List<AtendimentoResponse> response = atendimentoService.listar(
                1L,
                2L,
                data,
                null,
                null,
                StatusAtendimento.AGENDADO,
                null,
                autenticacaoGerente(),
                "Bearer token"
        );

        assertThat(response).hasSize(1);
        verify(atendimentoRepository).buscarAtivosComFiltros(1L, 2L, data, null, null, StatusAtendimento.AGENDADO, null);
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
                .hasMessage("Dentista pode acessar apenas os proprios atendimentos");
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


    @Test
    @DisplayName("deve buscar atendimento por id respeitando acesso do dentista")
    void deveBuscarAtendimentoPorId() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));
        when(profissionalClient.buscarMeuPerfil("Bearer token")).thenReturn(dentistaAtivo(2L));

        AtendimentoResponse response = atendimentoService.buscarPorId(10L, autenticacaoDentista(), "Bearer token");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.profissionalId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("deve realizar atendimento preenchendo dados clinicos e financeiros")
    void deveRealizarAtendimento() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(atendimento)).thenReturn(atendimento);

        AtendimentoResponse response = atendimentoService.realizar(
                10L,
                new RealizacaoAtendimentoRequest("Profilaxia", "Paciente sem queixas", BigDecimal.valueOf(150)),
                autenticacaoGerente(),
                "Bearer token"
        );

        assertThat(response.status()).isEqualTo(StatusAtendimento.REALIZADO);
        assertThat(response.procedimentoRealizado()).isEqualTo("Profilaxia");
        assertThat(response.observacoes()).isEqualTo("Paciente sem queixas");
        assertThat(response.valor()).isEqualByComparingTo("150");
        assertThat(response.realizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("deve impedir realizar atendimento cancelado")
    void deveImpedirRealizarAtendimentoCancelado() {
        Atendimento atendimento = atendimentoSalvo(2L);
        atendimento.setStatus(StatusAtendimento.CANCELADO);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> atendimentoService.realizar(
                10L,
                new RealizacaoAtendimentoRequest("Profilaxia", null, BigDecimal.valueOf(150)),
                autenticacaoGerente(),
                "Bearer token"
        )).isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Atendimento cancelado nao pode ser realizado");
    }

    @Test
    @DisplayName("deve cancelar atendimento agendado")
    void deveCancelarAtendimentoAgendado() {
        Atendimento atendimento = atendimentoSalvo(2L);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(atendimento)).thenReturn(atendimento);

        AtendimentoResponse response = atendimentoService.cancelar(10L, autenticacaoGerente(), "Bearer token");

        assertThat(response.status()).isEqualTo(StatusAtendimento.CANCELADO);
    }

    @Test
    @DisplayName("deve impedir cancelar atendimento realizado")
    void deveImpedirCancelarAtendimentoRealizado() {
        Atendimento atendimento = atendimentoSalvo(2L);
        atendimento.setStatus(StatusAtendimento.REALIZADO);
        when(atendimentoRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> atendimentoService.cancelar(10L, autenticacaoGerente(), "Bearer token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Atendimento realizado nao pode ser cancelado");
    }

    private AtendimentoRequest requestPadrao() {
        return new AtendimentoRequest(
                1L,
                2L,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                45,
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
        return atendimentoSalvo(profissionalId, 1L, LocalTime.of(9, 0), 60);
    }

    private Atendimento atendimentoSalvo(Long profissionalId, Long pacienteId, LocalTime hora, Integer duracaoMinutos) {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(10L);
        atendimento.setPacienteId(pacienteId);
        atendimento.setPacienteNome("Maria Silva");
        atendimento.setProfissionalId(profissionalId);
        atendimento.setProfissionalNome("Dr Joao");
        atendimento.setProfissionalEmail("joao@clinica.com");
        atendimento.setDataAtendimento(LocalDate.now().plusDays(1));
        atendimento.setHoraAtendimento(hora);
        atendimento.setDuracaoMinutos(duracaoMinutos);
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
