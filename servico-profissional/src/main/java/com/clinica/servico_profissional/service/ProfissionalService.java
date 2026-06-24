package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.ProfissionalRequest;
import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.exception.RecursoNaoEncontradoException;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;

    public List<ProfissionalResponse> listarAtivos() {
        return profissionalRepository.findByAtivoTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProfissionalResponse buscarPorId(Long id) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " não encontrado"
                ));
        return toResponse(profissional);
    }

    /**
     * Cadastra um novo profissional.
     *
     * TODO (US-11): integrar com o servico-autenticacao para criar o usuário
     * de login correspondente a este profissional (mesmo email/role).
     * Por ora, o cadastro fica restrito aos dados de domínio do profissional.
     */
    public ProfissionalResponse cadastrar(ProfissionalRequest request) {
        validarDadosObrigatorios(request);

        if (profissionalRepository.existsByEmail(request.getEmail())) {
            throw new RegraDeNegocioException(
                    "Já existe um profissional cadastrado com o email " + request.getEmail()
            );
        }

        Profissional profissional = new Profissional(
                null,
                request.getNome(),
                request.getEmail(),
                request.getCro(),
                request.getEspecialidade(),
                request.getRole(),
                true,
                LocalDateTime.now()
        );

        Profissional salvo = profissionalRepository.save(profissional);
        log.info("Profissional cadastrado com sucesso: {}", salvo.getEmail());

        // TODO (US-11): chamar servico-autenticacao para criar o login deste profissional.

        return toResponse(salvo);
    }

    /**
     * Atualiza os dados de domínio de um profissional já existente.
     * Não altera id, ativo ou criadoEm — esses campos não fazem parte
     * do contrato de atualização.
     */
    public ProfissionalResponse atualizar(Long id, ProfissionalRequest request) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " não encontrado"
                ));

        validarDadosObrigatorios(request);
        validarEmailDisponivel(request.getEmail(), profissional);

        profissional.setNome(request.getNome());
        profissional.setEmail(request.getEmail());
        profissional.setCro(request.getCro());
        profissional.setEspecialidade(request.getEspecialidade());
        profissional.setRole(request.getRole());

        Profissional atualizado = profissionalRepository.save(profissional);
        log.info("Profissional atualizado com sucesso: id={}", atualizado.getId());

        return toResponse(atualizado);
    }

    /**
     * Inativa um profissional (soft delete). O registro nunca é removido
     * do banco — apenas marcado como ativo = false, conforme regra do projeto.
     */
    public void inativar(Long id) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " não encontrado"
                ));

        profissional.setAtivo(false);
        profissionalRepository.save(profissional);

        log.info("Profissional inativado com sucesso: id={}", id);
    }

    private void validarEmailDisponivel(String novoEmail, Profissional profissionalAtual) {
        boolean emailMudou = !novoEmail.equalsIgnoreCase(profissionalAtual.getEmail());

        if (emailMudou && profissionalRepository.existsByEmail(novoEmail)) {
            throw new RegraDeNegocioException(
                    "Já existe um profissional cadastrado com o email " + novoEmail
            );
        }
    }

    private void validarDadosObrigatorios(ProfissionalRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new RegraDeNegocioException("O campo nome é obrigatório.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O campo email é obrigatório.");
        }
        if (request.getRole() == null) {
            throw new RegraDeNegocioException("O campo role é obrigatório.");
        }
    }

    private ProfissionalResponse toResponse(Profissional profissional) {
        return new ProfissionalResponse(
                profissional.getId(),
                profissional.getNome(),
                profissional.getEmail(),
                profissional.getCro(),
                profissional.getEspecialidade(),
                profissional.getRole(),
                profissional.isAtivo()
        );
    }
}