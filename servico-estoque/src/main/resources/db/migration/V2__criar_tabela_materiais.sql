CREATE TABLE IF NOT EXISTS materiais (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    categoria VARCHAR(80),
    unidade_medida VARCHAR(20) NOT NULL,
    quantidade_atual DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantidade_minima DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME,
    CONSTRAINT uk_materiais_nome UNIQUE (nome)
);

CREATE INDEX idx_materiais_categoria ON materiais (categoria);
CREATE INDEX idx_materiais_ativo ON materiais (ativo);
