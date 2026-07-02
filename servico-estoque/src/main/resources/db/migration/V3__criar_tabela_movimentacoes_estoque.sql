CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade DECIMAL(10,2) NOT NULL,
    saldo_anterior DECIMAL(10,2) NOT NULL,
    saldo_atual DECIMAL(10,2) NOT NULL,
    motivo VARCHAR(500),
    usuario_email VARCHAR(150),
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimentacoes_estoque_material
        FOREIGN KEY (material_id) REFERENCES materiais(id)
);

CREATE INDEX idx_movimentacoes_estoque_material ON movimentacoes_estoque(material_id);
CREATE INDEX idx_movimentacoes_estoque_criado_em ON movimentacoes_estoque(criado_em);
