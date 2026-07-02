package com.clinica.servico_estoque.service;

import com.clinica.servico_estoque.dto.MovimentacaoEstoqueRequest;
import com.clinica.servico_estoque.dto.MovimentacaoEstoqueResponse;
import com.clinica.servico_estoque.exception.RecursoNaoEncontradoException;
import com.clinica.servico_estoque.exception.RegraDeNegocioException;
import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.MovimentacaoEstoque;
import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;
import com.clinica.servico_estoque.repository.MaterialRepository;
import com.clinica.servico_estoque.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public MovimentacaoEstoqueResponse registrar(
            Long materialId,
            MovimentacaoEstoqueRequest request,
            Authentication authentication
    ) {
        Material material = buscarMaterialAtivo(materialId);
        validarRequest(request);

        BigDecimal saldoAnterior = material.getQuantidadeAtual();
        BigDecimal saldoAtual = calcularSaldoAtual(saldoAnterior, request.tipo(), request.quantidade());

        material.setQuantidadeAtual(saldoAtual);
        materialRepository.save(material);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setTipo(request.tipo());
        movimentacao.setQuantidade(request.quantidade());
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoAtual(saldoAtual);
        movimentacao.setMotivo(normalizarTextoOpcional(request.motivo()));
        movimentacao.setUsuarioEmail(resolverUsuarioEmail(authentication));

        return MovimentacaoEstoqueResponse.from(movimentacaoEstoqueRepository.save(movimentacao));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponse> listarPorMaterial(Long materialId) {
        buscarMaterialAtivo(materialId);
        return movimentacaoEstoqueRepository.findByMaterialIdOrderByCriadoEmDesc(materialId)
                .stream()
                .map(MovimentacaoEstoqueResponse::from)
                .toList();
    }

    private Material buscarMaterialAtivo(Long materialId) {
        return materialRepository.findByIdAndAtivoTrue(materialId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material nao encontrado"));
    }

    private void validarRequest(MovimentacaoEstoqueRequest request) {
        if (request.tipo() == null) {
            throw new RegraDeNegocioException("Tipo de movimentacao e obrigatorio");
        }

        if (request.quantidade() == null) {
            throw new RegraDeNegocioException("Quantidade e obrigatoria");
        }

        if (request.tipo() == TipoMovimentacaoEstoque.AJUSTE) {
            if (request.quantidade().compareTo(BigDecimal.ZERO) < 0) {
                throw new RegraDeNegocioException("Quantidade nao pode ser negativa");
            }
            validarMotivoObrigatorio(request.motivo(), "Motivo e obrigatorio para ajuste de estoque");
            return;
        }

        if (request.quantidade().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Quantidade deve ser maior que zero");
        }

        if (request.tipo() == TipoMovimentacaoEstoque.SAIDA) {
            validarMotivoObrigatorio(request.motivo(), "Motivo e obrigatorio para saida de estoque");
        }
    }

    private BigDecimal calcularSaldoAtual(BigDecimal saldoAnterior, TipoMovimentacaoEstoque tipo, BigDecimal quantidade) {
        if (tipo == TipoMovimentacaoEstoque.ENTRADA) {
            return saldoAnterior.add(quantidade);
        }

        if (tipo == TipoMovimentacaoEstoque.AJUSTE) {
            return quantidade;
        }

        BigDecimal saldoAtual = saldoAnterior.subtract(quantidade);
        if (saldoAtual.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("Saida nao pode deixar estoque negativo");
        }
        return saldoAtual;
    }

    private void validarMotivoObrigatorio(String valor, String mensagem) {
        if (normalizarTextoOpcional(valor) == null) {
            throw new RegraDeNegocioException(mensagem);
        }
    }

    private String resolverUsuarioEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return normalizarTextoOpcional(authentication.getName());
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}
