package com.clinica.servico_estoque.service;

import com.clinica.servico_estoque.dto.MovimentacaoEstoqueRequest;
import com.clinica.servico_estoque.dto.MovimentacaoEstoqueResponse;
import com.clinica.servico_estoque.exception.RegraDeNegocioException;
import com.clinica.servico_estoque.model.Material;
import com.clinica.servico_estoque.model.MovimentacaoEstoque;
import com.clinica.servico_estoque.model.TipoMovimentacaoEstoque;
import com.clinica.servico_estoque.model.UnidadeMedida;
import com.clinica.servico_estoque.repository.MaterialRepository;
import com.clinica.servico_estoque.repository.MovimentacaoEstoqueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovimentacaoEstoqueService")
class MovimentacaoEstoqueServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @InjectMocks
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Test
    @DisplayName("deve registrar entrada somando ao saldo")
    void deveRegistrarEntradaSomandoSaldo() {
        Material material = materialSalvo(BigDecimal.TEN);
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> {
            MovimentacaoEstoque movimentacao = invocation.getArgument(0);
            movimentacao.setId(10L);
            return movimentacao;
        });

        MovimentacaoEstoqueResponse response = movimentacaoEstoqueService.registrar(
                1L,
                new MovimentacaoEstoqueRequest(TipoMovimentacaoEstoque.ENTRADA, BigDecimal.valueOf(5), "Compra"),
                new TestingAuthenticationToken("auxiliar@clinica.com", null)
        );

        assertThat(response.saldoAnterior()).isEqualByComparingTo("10");
        assertThat(response.saldoAtual()).isEqualByComparingTo("15");
        assertThat(response.usuarioEmail()).isEqualTo("auxiliar@clinica.com");
        assertThat(material.getQuantidadeAtual()).isEqualByComparingTo("15");

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA);
    }

    @Test
    @DisplayName("deve registrar saida subtraindo do saldo")
    void deveRegistrarSaidaSubtraindoSaldo() {
        Material material = materialSalvo(BigDecimal.TEN);
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovimentacaoEstoqueResponse response = movimentacaoEstoqueService.registrar(
                1L,
                new MovimentacaoEstoqueRequest(TipoMovimentacaoEstoque.SAIDA, BigDecimal.valueOf(3), "Uso em atendimento"),
                null
        );

        assertThat(response.saldoAtual()).isEqualByComparingTo("7");
        assertThat(material.getQuantidadeAtual()).isEqualByComparingTo("7");
    }

    @Test
    @DisplayName("deve bloquear saida que deixa estoque negativo")
    void deveBloquearSaidaComEstoqueNegativo() {
        Material material = materialSalvo(BigDecimal.valueOf(2));
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> movimentacaoEstoqueService.registrar(
                1L,
                new MovimentacaoEstoqueRequest(TipoMovimentacaoEstoque.SAIDA, BigDecimal.valueOf(3), "Uso"),
                null
        ))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Saida nao pode deixar estoque negativo");

        verify(materialRepository, never()).save(any(Material.class));
        verify(movimentacaoEstoqueRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("deve registrar ajuste definindo novo saldo")
    void deveRegistrarAjusteDefinindoNovoSaldo() {
        Material material = materialSalvo(BigDecimal.TEN);
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovimentacaoEstoqueResponse response = movimentacaoEstoqueService.registrar(
                1L,
                new MovimentacaoEstoqueRequest(TipoMovimentacaoEstoque.AJUSTE, BigDecimal.ZERO, "Inventario"),
                null
        );

        assertThat(response.saldoAtual()).isEqualByComparingTo("0");
        assertThat(material.getQuantidadeAtual()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("deve exigir motivo para saida")
    void deveExigirMotivoParaSaida() {
        Material material = materialSalvo(BigDecimal.TEN);
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> movimentacaoEstoqueService.registrar(
                1L,
                new MovimentacaoEstoqueRequest(TipoMovimentacaoEstoque.SAIDA, BigDecimal.ONE, " "),
                null
        ))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Motivo e obrigatorio para saida de estoque");
    }

    @Test
    @DisplayName("deve listar movimentacoes por material")
    void deveListarMovimentacoesPorMaterial() {
        Material material = materialSalvo(BigDecimal.TEN);
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setId(1L);
        movimentacao.setMaterial(material);
        movimentacao.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao.setQuantidade(BigDecimal.ONE);
        movimentacao.setSaldoAnterior(BigDecimal.TEN);
        movimentacao.setSaldoAtual(BigDecimal.valueOf(11));
        when(materialRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(material));
        when(movimentacaoEstoqueRepository.findByMaterialIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(movimentacao));

        List<MovimentacaoEstoqueResponse> response = movimentacaoEstoqueService.listarPorMaterial(1L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().materialNome()).isEqualTo("Anestesico");
    }

    private Material materialSalvo(BigDecimal quantidadeAtual) {
        Material material = new Material();
        material.setId(1L);
        material.setNome("Anestesico");
        material.setDescricao("Tubete odontologico");
        material.setCategoria("Medicamento");
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeAtual(quantidadeAtual);
        material.setQuantidadeMinima(BigDecimal.TEN);
        material.setAtivo(true);
        return material;
    }
}
