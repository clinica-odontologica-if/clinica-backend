CREATE TABLE IF NOT EXISTS atendimentos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    paciente_id BIGINT NOT NULL,
    paciente_nome VARCHAR(120) NOT NULL,
    profissional_id BIGINT NOT NULL,
    profissional_nome VARCHAR(120) NOT NULL,
    profissional_email VARCHAR(150) NOT NULL,
    data_atendimento DATE NOT NULL,
    hora_atendimento TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    observacoes VARCHAR(500),
    procedimento_realizado VARCHAR(255),
    valor DECIMAL(10, 2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME,
    realizado_em DATETIME,

    PRIMARY KEY (id),
    CONSTRAINT ck_atendimentos_status CHECK (
        status IN ('AGENDADO', 'CONFIRMADO', 'REALIZADO', 'CANCELADO', 'NAO_COMPARECEU')
    )
);

CREATE INDEX idx_atendimentos_paciente
    ON atendimentos (paciente_id);

CREATE INDEX idx_atendimentos_profissional_data_hora
    ON atendimentos (profissional_id, data_atendimento, hora_atendimento);

CREATE INDEX idx_atendimentos_status
    ON atendimentos (status);
