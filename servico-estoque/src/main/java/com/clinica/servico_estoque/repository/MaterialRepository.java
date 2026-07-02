package com.clinica.servico_estoque.repository;

import com.clinica.servico_estoque.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByIdAndAtivoTrue(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("""
            SELECT m
            FROM Material m
            WHERE (:ativo IS NULL OR m.ativo = :ativo)
              AND (:busca IS NULL OR
                   LOWER(m.nome) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(m.descricao) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                   LOWER(m.categoria) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:categoria IS NULL OR LOWER(m.categoria) = LOWER(:categoria))
            ORDER BY m.nome ASC
            """)
    List<Material> buscarComFiltros(@Param("busca") String busca,
                                    @Param("categoria") String categoria,
                                    @Param("ativo") Boolean ativo);
}
