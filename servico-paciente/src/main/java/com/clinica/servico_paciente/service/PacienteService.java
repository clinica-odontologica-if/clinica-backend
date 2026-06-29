package com.clinica.servico_paciente.service;

import com.clinica.servico_paciente.dto.PacienteRequest;
import com.clinica.servico_paciente.dto.PacienteResponse;
import com.clinica.servico_paciente.exception.RecursoNaoEncontradoException;
import com.clinica.servico_paciente.exception.RegraDeNegocioException;
import com.clinica.servico_paciente.model.Paciente;
import com.clinica.servico_paciente.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public List<PacienteResponse> listarAtivos() {
        return pacienteRepository.findByAtivoTrue()
                .stream()
                .map(PacienteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(Long id) {
        return PacienteResponse.from(buscarPacienteAtivo(id));
    }

    @Transactional
    public PacienteResponse cadastrar(PacienteRequest request) {
        String cpf = normalizarCpf(request.cpf());
        validarCpf(cpf);
        validarTelefone(request.telefone());
        validarDataNascimento(request.dataNascimento());

        if (pacienteRepository.existsByCpf(cpf)) {
            throw new RegraDeNegocioException("Ja existe paciente cadastrado com este CPF");
        }

        validarEmailUnico(request.email(), null);

        Paciente paciente = new Paciente();
        aplicarDados(paciente, request, cpf);
        paciente.setAtivo(true);

        return PacienteResponse.from(pacienteRepository.save(paciente));
    }

    @Transactional
    public PacienteResponse atualizar(Long id, PacienteRequest request) {
        Paciente paciente = buscarPacienteAtivo(id);
        String cpf = normalizarCpf(request.cpf());
        validarCpf(cpf);
        validarTelefone(request.telefone());
        validarDataNascimento(request.dataNascimento());

        if (pacienteRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new RegraDeNegocioException("Ja existe paciente cadastrado com este CPF");
        }

        validarEmailUnico(request.email(), id);

        aplicarDados(paciente, request, cpf);
        return PacienteResponse.from(pacienteRepository.save(paciente));
    }

    @Transactional
    public void inativar(Long id) {
        Paciente paciente = buscarPacienteAtivo(id);
        paciente.setAtivo(false);
        pacienteRepository.save(paciente);
    }

    private Paciente buscarPacienteAtivo(Long id) {
        return pacienteRepository.findById(id)
                .filter(Paciente::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }

    private void aplicarDados(Paciente paciente, PacienteRequest request, String cpf) {
        paciente.setNome(normalizarTextoObrigatorio(request.nome(), "Nome e obrigatorio"));
        paciente.setCpf(cpf);
        paciente.setDataNascimento(request.dataNascimento());
        paciente.setTelefone(normalizarTelefone(request.telefone()));
        paciente.setEmail(normalizarEmail(request.email()));
        paciente.setEndereco(normalizarTextoOpcional(request.endereco()));
        paciente.setObservacoes(normalizarTextoOpcional(request.observacoes()));
    }

    private void validarDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            throw new RegraDeNegocioException("Data de nascimento e obrigatoria");
        }
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("Data de nascimento nao pode ser futura");
        }
    }

    private void validarTelefone(String telefone) {
        String telefoneNormalizado = normalizarTelefone(telefone);
        if (telefoneNormalizado.length() < 10 || telefoneNormalizado.length() > 11) {
            throw new RegraDeNegocioException("Telefone deve ter 10 ou 11 digitos");
        }
    }

    private void validarEmailUnico(String email, Long idAtual) {
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado == null) {
            return;
        }

        boolean emailEmUso = idAtual == null
                ? pacienteRepository.existsByEmail(emailNormalizado)
                : pacienteRepository.existsByEmailAndIdNot(emailNormalizado, idAtual);

        if (emailEmUso) {
            throw new RegraDeNegocioException("Ja existe paciente cadastrado com este email");
        }
    }

    private String normalizarCpf(String cpf) {
        return somenteDigitos(cpf);
    }

    private String normalizarTelefone(String telefone) {
        return somenteDigitos(telefone);
    }

    private String somenteDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\D", "");
    }

    private String normalizarEmail(String email) {
        String valor = normalizarTextoOpcional(email);
        return valor == null ? null : valor.toLowerCase();
    }

    private String normalizarTextoObrigatorio(String valor, String mensagem) {
        String texto = normalizarTextoOpcional(valor);
        if (texto == null) {
            throw new RegraDeNegocioException(mensagem);
        }
        return texto;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    private void validarCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            throw new RegraDeNegocioException("CPF invalido");
        }

        int primeiroDigito = calcularDigitoCpf(cpf, 9);
        int segundoDigito = calcularDigitoCpf(cpf, 10);

        if (primeiroDigito != Character.getNumericValue(cpf.charAt(9))
                || segundoDigito != Character.getNumericValue(cpf.charAt(10))) {
            throw new RegraDeNegocioException("CPF invalido");
        }
    }

    private int calcularDigitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
