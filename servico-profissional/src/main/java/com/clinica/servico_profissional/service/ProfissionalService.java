package com.clinica.servico_profissional.service;

import com.clinica.servico_profissional.dto.ProfissionalRequest;
import com.clinica.servico_profissional.dto.ProfissionalResponse;
import com.clinica.servico_profissional.dto.UsuarioInternoRequest;
import com.clinica.servico_profissional.exception.RecursoNaoEncontradoException;
import com.clinica.servico_profissional.exception.RegraDeNegocioException;
import com.clinica.servico_profissional.model.Profissional;
import com.clinica.servico_profissional.model.Role;
import com.clinica.servico_profissional.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final AutenticacaoClientService autenticacaoClientService;

    public List<ProfissionalResponse> listarAtivos() {
        return listarAtivos(null, null, null);
    }

    public List<ProfissionalResponse> listarAtivos(String busca, String especialidade, Role role) {
        return profissionalRepository.buscarAtivosComFiltros(
                        normalizarFiltro(busca),
                        normalizarFiltro(especialidade),
                        role
                )
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

    public ProfissionalResponse buscarPorEmail(String email) {
        String emailNormalizado = normalizarEmail(email);
        Profissional profissional = profissionalRepository.findByEmail(emailNormalizado)
                .filter(Profissional::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com email " + emailNormalizado + " nao encontrado"
                ));
        return toResponse(profissional);
    }

    @Transactional
    public ProfissionalResponse cadastrar(ProfissionalRequest request) {
        validarCadastro(request);

        String email = normalizarEmail(request.getEmail());
        if (profissionalRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Ja existe um profissional com este email.");
        }

        String senhaInicial = gerarSenhaInicial(request.getNome());
        autenticacaoClientService.criarUsuarioInterno(new UsuarioInternoRequest(
                request.getNome().trim(),
                email,
                senhaInicial,
                request.getRole()
        ));

        Profissional profissional = new Profissional();
        profissional.setNome(request.getNome().trim());
        profissional.setEmail(email);
        profissional.setRole(request.getRole());
        aplicarCamposDentista(profissional, request);

        return toResponse(profissionalRepository.save(profissional));
    }

    @Transactional
    public ProfissionalResponse atualizar(Long id, ProfissionalRequest request) {
        validarCadastro(request);

        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " nao encontrado"
                ));

        String email = normalizarEmail(request.getEmail());
        if (!profissional.getEmail().equalsIgnoreCase(email)) {
            throw new RegraDeNegocioException("Email do profissional nao pode ser alterado nesta sprint.");
        }
        if (profissional.getRole() != request.getRole()) {
            throw new RegraDeNegocioException("Perfil do profissional nao pode ser alterado nesta sprint.");
        }

        profissional.setNome(request.getNome().trim());
        aplicarCamposDentista(profissional, request);

        return toResponse(profissionalRepository.save(profissional));
    }

    @Transactional
    public void inativar(Long id) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional com id " + id + " nao encontrado"
                ));

        profissional.setAtivo(false);
        profissionalRepository.save(profissional);
        autenticacaoClientService.inativarUsuarioInterno(profissional.getEmail());
    }

    private void validarCadastro(ProfissionalRequest request) {
        if (request == null) {
            throw new RegraDeNegocioException("Dados do profissional sao obrigatorios.");
        }
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new RegraDeNegocioException("O campo nome e obrigatorio.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O campo email e obrigatorio.");
        }
        if (!request.getEmail().contains("@")) {
            throw new RegraDeNegocioException("O campo email deve ser valido.");
        }
        if (request.getRole() == null) {
            throw new RegraDeNegocioException("O campo role e obrigatorio.");
        }
        if (request.getRole() == Role.DENTISTA) {
            if (request.getCro() == null || request.getCro().isBlank()) {
                throw new RegraDeNegocioException("O campo cro e obrigatorio para dentistas.");
            }
            if (request.getEspecialidade() == null || request.getEspecialidade().isBlank()) {
                throw new RegraDeNegocioException("O campo especialidade e obrigatorio para dentistas.");
            }
        }
    }

    private void aplicarCamposDentista(Profissional profissional, ProfissionalRequest request) {
        if (request.getRole() == Role.DENTISTA) {
            profissional.setCro(request.getCro().trim());
            profissional.setEspecialidade(request.getEspecialidade().trim());
            return;
        }

        profissional.setCro(null);
        profissional.setEspecialidade(null);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizarFiltro(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim().toLowerCase();
        return texto.isBlank() ? null : texto;
    }

    private String gerarSenhaInicial(String nome) {
        String primeiroNome = nome.trim().split("\\s+")[0];
        String semAcentos = Normalizer.normalize(primeiroNome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "");

        if (semAcentos.isBlank()) {
            semAcentos = "Usuario";
        }

        return semAcentos + "@" + Year.now().getValue();
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
