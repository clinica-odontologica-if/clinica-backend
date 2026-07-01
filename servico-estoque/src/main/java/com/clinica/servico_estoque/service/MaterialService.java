package com.clinica.servico_estoque.service;

import com.clinica.servico_estoque.dto.MaterialRequest;
import com.clinica.servico_estoque.dto.MaterialResponse;
import com.clinica.servico_estoque.exception.RecursoNaoEncontradoException;
import com.clinica.servico_estoque.exception.RegraDeNegocioException;
import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> listar(String busca, String categoria, Boolean baixoEstoque, Boolean ativo) {
        Boolean filtroAtivo = ativo != null ? ativo : true;
        return materialRepository.buscarComFiltros(
                        normalizarFiltroTexto(busca),
                        normalizarFiltroTexto(categoria),
                        filtroAtivo
                )
                .stream()
                .filter(material -> baixoEstoque == null || Objects.equals(material.isBaixoEstoque(), baixoEstoque))
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarBaixoEstoque() {
        return materialRepository.buscarComFiltros(null, null, true)
                .stream()
                .filter(Material::isBaixoEstoque)
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse buscarPorId(Long id) {
        return MaterialResponse.from(buscarMaterialAtivo(id));
    }

    @Transactional
    public MaterialResponse cadastrar(MaterialRequest request) {
        validarQuantidades(request.quantidadeAtual(), request.quantidadeMinima());
        String nome = normalizarTextoObrigatorio(request.nome(), "Nome e obrigatorio");

        if (materialRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Ja existe material cadastrado com este nome");
        }

        Material material = new Material();
        aplicarDados(material, request, nome);
        material.setAtivo(true);

        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse atualizar(Long id, MaterialRequest request) {
        Material material = buscarMaterialAtivo(id);
        validarQuantidades(request.quantidadeAtual(), request.quantidadeMinima());
        String nome = normalizarTextoObrigatorio(request.nome(), "Nome e obrigatorio");

        if (materialRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException("Ja existe material cadastrado com este nome");
        }

        aplicarDados(material, request, nome);
        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public void inativar(Long id) {
        Material material = buscarMaterialAtivo(id);
        material.setAtivo(false);
        materialRepository.save(material);
    }

    private Material buscarMaterialAtivo(Long id) {
        return materialRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material nao encontrado"));
    }

    private void aplicarDados(Material material, MaterialRequest request, String nome) {
        material.setNome(nome);
        material.setDescricao(normalizarTextoOpcional(request.descricao()));
        material.setCategoria(normalizarTextoOpcional(request.categoria()));
        material.setUnidadeMedida(request.unidadeMedida());
        material.setQuantidadeAtual(resolverQuantidadeAtual(request.quantidadeAtual()));
        material.setQuantidadeMinima(request.quantidadeMinima());
    }

    private void validarQuantidades(BigDecimal quantidadeAtual, BigDecimal quantidadeMinima) {
        if (quantidadeAtual != null && quantidadeAtual.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Quantidade atual nao pode ser negativa");
        }

        if (quantidadeMinima == null) {
            throw new RegraDeNegocioException("Quantidade minima e obrigatoria");
        }

        if (quantidadeMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Quantidade minima nao pode ser negativa");
        }
    }

    private BigDecimal resolverQuantidadeAtual(BigDecimal quantidadeAtual) {
        return quantidadeAtual != null ? quantidadeAtual : BigDecimal.ZERO;
    }

    private String normalizarFiltroTexto(String valor) {
        String texto = normalizarTextoOpcional(valor);
        return texto == null ? null : texto.toLowerCase();
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
}
