-- ============================================================================
-- SCRIPT DE CARGA DE DADOS OFICIAIS - PROJETO LORY (v2.0)
-- Execute este script no MySQL Workbench conectado ao servidor lory_db
-- ============================================================================

USE lory_db;

-- Desativar temporariamente a verificação de chaves estrangeiras para limpeza
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Limpeza de dados antigos/testes
TRUNCATE TABLE notificacoes;
TRUNCATE TABLE votos;
TRUNCATE TABLE opcoes_enquete;
TRUNCATE TABLE enquetes;
TRUNCATE TABLE reunioes;
TRUNCATE TABLE ficheiros;
TRUNCATE TABLE presencas;
TRUNCATE TABLE sessoes;
TRUNCATE TABLE eventos;
TRUNCATE TABLE grupo_membros;
TRUNCATE TABLE grupos;
TRUNCATE TABLE inscricoes;
TRUNCATE TABLE cadeiras;
TRUNCATE TABLE semestres;
TRUNCATE TABLE docentes;
TRUNCATE TABLE utilizadores;
TRUNCATE TABLE turmas;
TRUNCATE TABLE instituicoes;

SET FOREIGN_KEY_CHECKS = 1;

-- 2. Cadastro da Instituição Oficial (Suporte Multi-tenant SaaS)
INSERT INTO instituicoes (id, nome, codigo_identificador, ativa) VALUES
('inst-001', 'Universidade Rovuma (UniRovuma)', 'UNIROVUMA_2026', TRUE);

-- 3. Cadastro da Turma Oficial
INSERT INTO turmas (id, instituicao_id, nome, ano_lectivo) VALUES
('turma-inf-2026', 'inst-001', 'Engenharia de Informática - 4º Ano', 2026);

-- 4. Cadastro do Chefe de Turma Real
-- Senha Padrão Inicial: 'lory2026' (Hash SHA-256 com Salt 'lory_salt_2026')
INSERT INTO utilizadores (id, turma_id, nome, email, senha_hash, salt, papel, estado) VALUES
('user-chefe-001', 'turma-inf-2026', 'Edilson Bachmann', 'edilson@lory.ac.mz',
 SHA2(CONCAT('lory2026', 'lory_salt_2026'), 256), 'lory_salt_2026', 'CHEFE', 'APROVADO');

-- 5. Cadastro de Docentes do Semestre
INSERT INTO docentes (id, turma_id, nome, contacto) VALUES
('doc-01', 'turma-inf-2026', 'Dr. António Silva', '+258 84 100 2001'),
('doc-02', 'turma-inf-2026', 'Prof. Msc. Beatriz Machava', '+258 84 100 2002'),
('doc-03', 'turma-inf-2026', 'Eng. Carlos Nhantumbo', '+258 84 100 2003'),
('doc-04', 'turma-inf-2026', 'Dr. Daniel Cossa', '+258 84 100 2004'),
('doc-05', 'turma-inf-2026', 'Prof. Eduardo Mondlane', '+258 84 100 2005'),
('doc-06', 'turma-inf-2026', 'Dra. Fátima Langa', '+258 84 100 2006'),
('doc-07', 'turma-inf-2026', 'Msc. Gabriel Mabunda', '+258 84 100 2007');

-- 6. Cadastro do Semestre Ativo
INSERT INTO semestres (id, turma_id, nome, activo) VALUES
('sem-2026-1', 'turma-inf-2026', '1º Semestre 2026', TRUE);

-- 7. Cadastro das 7 Cadeiras Reais do Curso
INSERT INTO cadeiras (id, turma_id, semestre_id, docente_id, nome, concluida) VALUES
('cad-01', 'turma-inf-2026', 'sem-2026-1', 'doc-01', 'Engenharia de Software II', FALSE),
('cad-02', 'turma-inf-2026', 'sem-2026-1', 'doc-02', 'Sistemas Distribuídos', FALSE),
('cad-03', 'turma-inf-2026', 'sem-2026-1', 'doc-03', 'Redes de Computadores II', FALSE),
('cad-04', 'turma-inf-2026', 'sem-2026-1', 'doc-04', 'Interacção Homem-Máquina', FALSE),
('cad-05', 'turma-inf-2026', 'sem-2026-1', 'doc-05', 'Sistemas Operativos Avançados', FALSE),
('cad-06', 'turma-inf-2026', 'sem-2026-1', 'doc-06', 'Segurança de Informação', FALSE),
('cad-07', 'turma-inf-2026', 'sem-2026-1', 'doc-07', 'Inteligência Artificial', FALSE);

-- 8. Enquetes e Votações Oficiais
INSERT INTO enquetes (id, turma_id, pergunta, prazo, criada_por) VALUES
('enq-01', 'turma-inf-2026', 'Data preferencial para o Teste 2 de Redes de Computadores?', '2026-11-15 23:59:59', 'user-chefe-001'),
('enq-02', 'turma-inf-2026', 'Modo de apresentação dos Trabalhos de Sistemas Distribuídos?', '2026-11-20 23:59:59', 'user-chefe-001');

-- Opções da Enquete 1
INSERT INTO opcoes_enquete (id, enquete_id, texto) VALUES
('op-01', 'enq-01', 'Terça-feira (10:00 - 12:00)'),
('op-02', 'enq-01', 'Quinta-feira (14:00 - 16:00)'),
('op-03', 'enq-01', 'Sexta-feira (08:00 - 10:00)');

-- Opções da Enquete 2
INSERT INTO opcoes_enquete (id, enquete_id, texto) VALUES
('op-04', 'enq-02', 'Presencial na Sala de Aulas'),
('op-05', 'enq-02', 'Apresentação Online via Google Meet');

-- 9. Eventos e Prazos Reais
INSERT INTO eventos (id, cadeira_id, grupo_id, tipo, titulo, data_evento, estado) VALUES
('ev-01', 'cad-01', NULL, 'TESTE', 'Entrega do Protótipo Lory - Fase 1', '2026-10-25 10:00:00', 'PENDENTE'),
('ev-02', 'cad-02', NULL, 'APRESENTACAO', 'Apresentação do Artigo de Sist. Distribuídos', '2026-11-05 14:00:00', 'PENDENTE'),
('ev-03', 'cad-03', NULL, 'TESTE', 'Teste 2 de Redes de Computadores II', '2026-11-18 09:00:00', 'PENDENTE');