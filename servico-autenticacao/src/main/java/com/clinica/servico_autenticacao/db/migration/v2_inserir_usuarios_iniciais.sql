-- =====================================================
-- Migração V2: Usuários iniciais do sistema
--
-- IMPORTANTE: Senhas geradas com BCrypt (12 rounds).
-- Troque as senhas antes de entregar ao cliente.
--
-- Senha padrão de cada usuário (alterar no primeiro login):
--   gerente@clinica.com   → Gerente@2025
--   atendente@clinica.com → Atendente@2025
--   dentista@clinica.com  → Dentista@2025
--   auxiliar@clinica.com  → Auxiliar@2025
-- =====================================================

INSERT INTO usuarios (nome, email, senha, role) VALUES
                                                    ('Gerente',
                                                     'gerente@clinica.com',
                                                     '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
                                                     'GERENTE'),

                                                    ('Atendente',
                                                     'atendente@clinica.com',
                                                     '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
                                                     'ATENDENTE'),

                                                    ('Dentista',
                                                     'dentista@clinica.com',
                                                     '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
                                                     'DENTISTA'),

                                                    ('Auxiliar',
                                                     'auxiliar@clinica.com',
                                                     '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
                                                     'AUXILIAR');