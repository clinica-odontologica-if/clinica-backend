package com.clinica.servico_estoque.repository;

import com.clinica.servico_estoque.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque> findByMaterialIdOrderByCriadoEmDesc(Long materialId);
}
