CREATE TABLE IF NOT EXISTS receitas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    atendimento_id BIGINT NOT NULL,
    paciente_id BIGINT,
    profissional_id BIGINT,
    descricao VARCHAR(255),
    valor DECIMAL(10,2) NOT NULL,
    forma_pagamento VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_vencimento DATE,
    data_pagamento DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME
);

CREATE INDEX idx_receitas_atendimento ON receitas(atendimento_id);
CREATE INDEX idx_receitas_paciente ON receitas(paciente_id);
CREATE INDEX idx_receitas_profissional ON receitas(profissional_id);
CREATE INDEX idx_receitas_status ON receitas(status);
CREATE INDEX idx_receitas_periodo ON receitas(data_pagamento, data_vencimento);

CREATE TABLE IF NOT EXISTS despesas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_vencimento DATE,
    data_pagamento DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME
);

CREATE INDEX idx_despesas_categoria ON despesas(categoria);
CREATE INDEX idx_despesas_status ON despesas(status);
CREATE INDEX idx_despesas_periodo ON despesas(data_pagamento, data_vencimento);