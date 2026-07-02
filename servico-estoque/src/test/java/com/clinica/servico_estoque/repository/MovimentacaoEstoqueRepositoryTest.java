package com.clinica.servico_estoque.repository;

import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.MovimentacaoEstoque;
import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;
import com.clinica.servico_estoque.model.UnidadeMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("MovimentacaoEstoqueRepository")
class MovimentacaoEstoqueRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Test
    @DisplayName("deve listar movimentacoes do material ordenando pelas mais recentes")
    void deveListarMovimentacoesDoMaterialOrdenadas() {
        Material material = materialRepository.saveAndFlush(material("Anestesico"));
        Material outroMaterial = materialRepository.saveAndFlush(material("Luva"));
        movimentacaoEstoqueRepository.save(movimentacao(material, BigDecimal.ONE, LocalDateTime.now().minusDays(1)));
        movimentacaoEstoqueRepository.save(movimentacao(material, BigDecimal.TEN, LocalDateTime.now()));
        movimentacaoEstoqueRepository.save(movimentacao(outroMaterial, BigDecimal.valueOf(3), LocalDateTime.now().plusDays(1)));
        movimentacaoEstoqueRepository.flush();

        List<MovimentacaoEstoque> encontradas = movimentacaoEstoqueRepository.findByMaterialIdOrderByCriadoEmDesc(material.getId());

        assertThat(encontradas).hasSize(2);
        assertThat(encontradas.getFirst().getQuantidade()).isEqualByComparingTo("10");
    }

    private Material material(String nome) {
        Material material = new Material();
        material.setNome(nome);
        material.setDescricao("Descricao " + nome);
        material.setCategoria("Medicamento");
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeAtual(BigDecimal.TEN);
        material.setQuantidadeMinima(BigDecimal.ONE);
        material.setAtivo(true);
        return material;
    }

    private MovimentacaoEstoque movimentacao(Material material, BigDecimal quantidade, LocalDateTime criadoEm) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setSaldoAnterior(BigDecimal.ZERO);
        movimentacao.setSaldoAtual(quantidade);
        movimentacao.setMotivo("Compra");
        movimentacao.setCriadoEm(criadoEm);
        return movimentacao;
    }
}
