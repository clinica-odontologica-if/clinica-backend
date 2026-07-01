package com.clinica.servico_estoque.repository;

import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.UnidadeMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("MaterialRepository")
class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Test
    @DisplayName("deve buscar materiais ativos com filtros")
    void deveBuscarMateriaisAtivosComFiltros() {
        materialRepository.saveAndFlush(material("Anestesico", "Medicamento", BigDecimal.valueOf(5), BigDecimal.TEN, true));
        materialRepository.saveAndFlush(material("Luva", "Descartavel", BigDecimal.valueOf(100), BigDecimal.valueOf(20), true));
        materialRepository.saveAndFlush(material("Mascara", "Descartavel", BigDecimal.ZERO, BigDecimal.TEN, false));

        List<Material> encontrados = materialRepository.buscarComFiltros("anes", "medicamento", true);

        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.getFirst().getNome()).isEqualTo("Anestesico");
    }

    @Test
    @DisplayName("deve listar materiais ativos por categoria")
    void deveListarMateriaisAtivosPorCategoria() {
        materialRepository.saveAndFlush(material("Anestesico", "Medicamento", BigDecimal.valueOf(5), BigDecimal.TEN, true));
        materialRepository.saveAndFlush(material("Luva", "Descartavel", BigDecimal.valueOf(100), BigDecimal.valueOf(20), true));

        List<Material> encontrados = materialRepository.buscarComFiltros(null, "descartavel", true);

        assertThat(encontrados).extracting(Material::getNome).containsExactly("Luva");
    }

    private Material material(String nome, String categoria, BigDecimal atual, BigDecimal minima, boolean ativo) {
        Material material = new Material();
        material.setNome(nome);
        material.setDescricao("Descricao " + nome);
        material.setCategoria(categoria);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeAtual(atual);
        material.setQuantidadeMinima(minima);
        material.setAtivo(ativo);
        return material;
    }
}
