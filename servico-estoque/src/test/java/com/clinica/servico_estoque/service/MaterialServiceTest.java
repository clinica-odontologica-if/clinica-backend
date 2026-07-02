package com.clinica.servico_estoque.service;

import com.clinica.servico_estoque.dto.MaterialRequest;
import com.clinica.servico_estoque.dto.MaterialResponse;
import com.clinica.servico_estoque.exception.RegraDeNegocioException;
import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.UnidadeMedida;
import com.clinica.servico_estoque.repository.MaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialService")
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialService materialService;

    @Test
    @DisplayName("deve cadastrar material normalizando campos e calculando baixo estoque")
    void deveCadastrarMaterial() {
        MaterialRequest request = requestPadrao();
        when(materialRepository.existsByNomeIgnoreCase("Anestesico")).thenReturn(false);
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            material.setId(1L);
            return material;
        });

        MaterialResponse response = materialService.cadastrar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Anestesico");
        assertThat(response.baixoEstoque()).isTrue();

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isEqualTo("Medicamento");
        assertThat(captor.getValue().getQuantidadeAtual()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("deve bloquear cadastro com nome duplicado")
    void deveBloquearNomeDuplicado() {
        MaterialRequest request = requestPadrao();
        when(materialRepository.existsByNomeIgnoreCase("Anestesico")).thenReturn(true);

        assertThatThrownBy(() -> materialService.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Ja existe material cadastrado com este nome");
    }

    @Test
    @DisplayName("deve bloquear quantidade atual negativa")
    void deveBloquearQuantidadeAtualNegativa() {
        MaterialRequest request = new MaterialRequest(
                "Anestesico",
                "Tubete",
                "Medicamento",
                UnidadeMedida.UNIDADE,
                BigDecimal.valueOf(-1),
                BigDecimal.TEN
        );

        assertThatThrownBy(() -> materialService.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Quantidade atual nao pode ser negativa");
    }

    @Test
    @DisplayName("deve listar materiais ativos com filtros normalizados")
    void deveListarMateriaisComFiltros() {
        Material material = materialSalvo();
        when(materialRepository.buscarComFiltros("anestesico", "medicamento", true))
                .thenReturn(List.of(material));

        List<MaterialResponse> response = materialService.listar(" Anestesico ", " Medicamento ", true, null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().nome()).isEqualTo("Anestesico");
        verify(materialRepository).buscarComFiltros("anestesico", "medicamento", true);
    }

    @Test
    @DisplayName("deve listar baixo estoque")
    void deveListarBaixoEstoque() {
        Material material = materialSalvo();
        when(materialRepository.buscarComFiltros(null, null, true)).thenReturn(List.of(material));

        List<MaterialResponse> response = materialService.listarBaixoEstoque();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().baixoEstoque()).isTrue();
    }

    @Test
    @DisplayName("deve atualizar material ativo")
    void deveAtualizarMaterial() {
        Material material = materialSalvo();
        MaterialRequest request = new MaterialRequest(
                "Anestesico 2",
                "Nova descricao",
                "Medicamento",
                UnidadeMedida.CAIXA,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5)
        );
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));
        when(materialRepository.existsByNomeIgnoreCaseAndIdNot("Anestesico 2", 1L)).thenReturn(false);
        when(materialRepository.save(material)).thenReturn(material);

        MaterialResponse response = materialService.atualizar(1L, request);

        assertThat(response.nome()).isEqualTo("Anestesico 2");
        assertThat(response.unidadeMedida()).isEqualTo(UnidadeMedida.CAIXA);
        assertThat(response.baixoEstoque()).isFalse();
    }

    @Test
    @DisplayName("deve inativar material ativo")
    void deveInativarMaterial() {
        Material material = materialSalvo();
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));

        materialService.inativar(1L);

        assertThat(material.isAtivo()).isFalse();
        verify(materialRepository).save(material);
    }

    private MaterialRequest requestPadrao() {
        return new MaterialRequest(
                " Anestesico ",
                " Tubete odontologico ",
                " Medicamento ",
                UnidadeMedida.UNIDADE,
                BigDecimal.valueOf(5),
                BigDecimal.TEN
        );
    }

    private Material materialSalvo() {
        Material material = new Material();
        material.setId(1L);
        material.setNome("Anestesico");
        material.setDescricao("Tubete odontologico");
        material.setCategoria("Medicamento");
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeAtual(BigDecimal.valueOf(5));
        material.setQuantidadeMinima(BigDecimal.TEN);
        material.setAtivo(true);
        return material;
    }
}
