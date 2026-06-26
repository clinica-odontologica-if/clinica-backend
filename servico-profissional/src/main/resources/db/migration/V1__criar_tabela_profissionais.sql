CREATE TABLE IF NOT EXISTS profissionais (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    cro VARCHAR(40),
    especialidade VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_profissionais_email UNIQUE (email),
    CONSTRAINT ck_profissionais_role CHECK (role IN ('GERENTE','ATENDENTE','DENTISTA','AUXILIAR'))
);
