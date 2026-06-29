package com.clinica.servico_atendimento.service;

import com.clinica.servico_atendimento.client.PacienteClient;
import com.clinica.servico_atendimento.client.PacienteClientResponse;
import com.clinica.servico_atendimento.client.ProfissionalClient;
import com.clinica.servico_atendimento.client.ProfissionalClientResponse;
import com.clinica.servico_atendimento.dto.AtendimentoRequest;
import com.clinica.servico_atendimento.dto.AtendimentoResponse;
import com.clinica.servico_atendimento.dto.StatusAtendimentoRequest;
import com.clinica.servico_atendimento.exception.RecursoNaoEncontradoException;
import com.clinica.servico_atendimento.exception.RegraDeNegocioException;
import com.clinica.servico_atendimento.model.Atendimento;
import com.clinica.servico_atendimento.model.StatusAtendimento;
import com.clinica.servico_atendimento.repository.AtendimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtendimentoService {

    private static final List<StatusAtendimento> STATUS_QUE_BLOQUEIAM_HORARIO = List.of(
            StatusAtendimento.AGENDADO,
            StatusAtendimento.CONFIRMADO
    );

    private final AtendimentoRepository atendimentoRepository;
    private final PacienteClient pacienteClient;
    private final ProfissionalClient profissionalClient;

    @Transactional
    public AtendimentoResponse cadastrar(AtendimentoRequest dto, String authorizationHeader) {
        PacienteClientResponse paciente = pacienteClient.buscarPorId(dto.pacienteId(), authorizationHeader);
        ProfissionalClientResponse profissional = profissionalClient.buscarPorId(dto.profissionalId(), authorizationHeader);

        validarRecursosAtivos(paciente, profissional);
        validarProfissionalDentista(profissional);
        validarConflitoDeHorario(dto);

        Atendimento atendimento = new Atendimento();
        atendimento.setPacienteId(paciente.id());
        atendimento.setPacienteNome(paciente.nome());
        atendimento.setProfissionalId(profissional.id());
        atendimento.setProfissionalNome(profissional.nome());
        atendimento.setProfissionalEmail(profissional.email());
        atendimento.setDataAtendimento(dto.data());
        atendimento.setHoraAtendimento(dto.hora());
        atendimento.setObservacoes(dto.observacoes());
        atendimento.setStatus(StatusAtendimento.AGENDADO);
        atendimento.setAtivo(true);

        return AtendimentoResponse.from(atendimentoRepository.save(atendimento));
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponse> listar(
            Long pacienteId,
            Long profissionalId,
            LocalDate data,
            StatusAtendimento status,
            Authentication authentication,
            String authorizationHeader
    ) {
        Long profissionalFiltro = resolverFiltroProfissional(profissionalId, authentication, authorizationHeader);

        return atendimentoRepository.buscarAtivosComFiltros(pacienteId, profissionalFiltro, data, status)
                .stream()
                .map(AtendimentoResponse::from)
                .toList();
    }

    @Transactional
    public AtendimentoResponse atualizarStatus(
            Long id,
            StatusAtendimentoRequest dto,
            Authentication authentication,
            String authorizationHeader
    ) {
        Atendimento atendimento = buscarAtendimentoAtivo(id);
        validarAcessoDoDentista(atendimento, authentication, authorizationHeader);
        validarMudancaDeStatus(atendimento, dto.status());

        atendimento.setStatus(dto.status());
        if (dto.status() == StatusAtendimento.REALIZADO && atendimento.getRealizadoEm() == null) {
            atendimento.setRealizadoEm(LocalDateTime.now());
        }

        return AtendimentoResponse.from(atendimentoRepository.save(atendimento));
    }

    private Atendimento buscarAtendimentoAtivo(Long id) {
        return atendimentoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento com id " + id + " nao encontrado"));
    }

    private void validarRecursosAtivos(PacienteClientResponse paciente, ProfissionalClientResponse profissional) {
        if (!paciente.ativo()) {
            throw new RegraDeNegocioException("Paciente informado esta inativo");
        }

        if (!profissional.ativo()) {
            throw new RegraDeNegocioException("Profissional informado esta inativo");
        }
    }

    private void validarProfissionalDentista(ProfissionalClientResponse profissional) {
        if (!"DENTISTA".equals(profissional.role())) {
            throw new RegraDeNegocioException("Atendimento deve ser vinculado a um profissional dentista");
        }
    }

    private void validarConflitoDeHorario(AtendimentoRequest dto) {
        boolean existeConflito = atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndHoraAtendimentoAndStatusInAndAtivoTrue(
                dto.profissionalId(),
                dto.data(),
                dto.hora(),
                STATUS_QUE_BLOQUEIAM_HORARIO
        );

        if (existeConflito) {
            throw new RegraDeNegocioException("Profissional ja possui atendimento agendado nesse horario");
        }
    }

    private Long resolverFiltroProfissional(
            Long profissionalId,
            Authentication authentication,
            String authorizationHeader
    ) {
        if (!isDentista(authentication)) {
            return profissionalId;
        }

        ProfissionalClientResponse profissionalLogado = profissionalClient.buscarMeuPerfil(authorizationHeader);
        return profissionalLogado.id();
    }

    private void validarAcessoDoDentista(
            Atendimento atendimento,
            Authentication authentication,
            String authorizationHeader
    ) {
        if (!isDentista(authentication)) {
            return;
        }

        ProfissionalClientResponse profissionalLogado = profissionalClient.buscarMeuPerfil(authorizationHeader);
        if (!atendimento.getProfissionalId().equals(profissionalLogado.id())) {
            throw new AccessDeniedException("Dentista pode alterar apenas os proprios atendimentos");
        }
    }

    private void validarMudancaDeStatus(Atendimento atendimento, StatusAtendimento novoStatus) {
        if (novoStatus == null) {
            throw new RegraDeNegocioException("Status e obrigatorio");
        }

        if (atendimento.getStatus() == StatusAtendimento.CANCELADO && novoStatus != StatusAtendimento.CANCELADO) {
            throw new RegraDeNegocioException("Atendimento cancelado nao pode mudar de status");
        }

        if (atendimento.getStatus() == StatusAtendimento.REALIZADO && novoStatus != StatusAtendimento.REALIZADO) {
            throw new RegraDeNegocioException("Atendimento realizado nao pode mudar de status");
        }
    }

    private boolean isDentista(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DENTISTA"::equals);
    }
}
