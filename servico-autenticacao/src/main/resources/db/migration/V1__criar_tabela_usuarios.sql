-- =====================================================
-- Migração V1: Criação da tabela de usuários
-- Autor: Time Clínica
-- Data: 2025-06
-- =====================================================

CREATE TABLE IF NOT EXISTS usuarios (
                                        id         BIGINT          NOT NULL AUTO_INCREMENT,
                                        nome       VARCHAR(100)    NOT NULL,
    email      VARCHAR(150)    NOT NULL,
    senha      VARCHAR(255)    NOT NULL,
    role       VARCHAR(20)     NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_role  CHECK (role IN ('GERENTE','ATENDENTE','DENTISTA','AUXILIAR'))
    );