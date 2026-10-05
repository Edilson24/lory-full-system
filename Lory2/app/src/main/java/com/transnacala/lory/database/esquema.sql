CREATE DATABASE IF NOT EXISTS lory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lory_db;

-- 1. Tabela de Instituições/Faculdades (Suporte a SaaS)
CREATE TABLE instituicoes (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    codigo_identificador VARCHAR(50) UNIQUE NOT NULL,
    ativa BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Turmas
CREATE TABLE turmas (
    id VARCHAR(36) PRIMARY KEY,
    instituicao_id VARCHAR(36) NOT NULL,
    nome VARCHAR(100) NOT NULL, -- Ex: "Informática 2026 - Pós-Laboral"
    ano_lectivo INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id) ON DELETE CASCADE
);

-- 3. Tabela de Utilizadores
CREATE TABLE utilizadores (
    id VARCHAR(36) PRIMARY KEY,
    turma_id VARCHAR(36) NULL, -- Nulo para SUPER_ADMIN ou ADMIN_INSTITUICAO
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    papel ENUM('SUPER_ADMIN', 'ADMIN_INSTITUICAO', 'CHEFE', 'ESTUDANTE') NOT NULL,
    estado ENUM('PENDENTE', 'APROVADO', 'REJEITADO') DEFAULT 'PENDENTE',
    data_nascimento DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (turma_id) REFERENCES turmas(id) ON DELETE SET NULL
);

-- 4. Docentes
CREATE TABLE docentes (
    id VARCHAR(36) PRIMARY KEY,
    turma_id VARCHAR(36) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    contacto VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (turma_id) REFERENCES turmas(id) ON DELETE CASCADE
);

-- 5. Semestres
CREATE TABLE semestres (
    id VARCHAR(36) PRIMARY KEY,
    turma_id VARCHAR(36) NOT NULL,
    nome VARCHAR(50) NOT NULL, -- Ex: "1º Semestre 2026"
    activo BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (turma_id) REFERENCES turmas(id) ON DELETE CASCADE
);

-- 6. Cadeiras
CREATE TABLE cadeiras (
    id VARCHAR(36) PRIMARY KEY,
    turma_id VARCHAR(36) NOT NULL,
    semestre_id VARCHAR(36) NOT NULL,
    docente_id VARCHAR(36) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    concluida BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (turma_id) REFERENCES turmas(id) ON DELETE CASCADE,
    FOREIGN KEY (semestre_id) REFERENCES semestres(id) ON DELETE CASCADE,
    FOREIGN KEY (docente_id) REFERENCES docentes(id) ON DELETE CASCADE
);

-- 7. Inscrição de Estudantes nas Cadeiras
CREATE TABLE inscricoes (
    cadeira_id VARCHAR(36) NOT NULL,
    estudante_id VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cadeira_id, estudante_id),
    FOREIGN KEY (cadeira_id) REFERENCES cadeiras(id) ON DELETE CASCADE,
    FOREIGN KEY (estudante_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 8. Grupos
CREATE TABLE grupos (
    id VARCHAR(36) PRIMARY KEY,
    cadeira_id VARCHAR(36) NOT NULL,
    nome VARCHAR(50) NOT NULL,
    tema VARCHAR(255) NULL,
    ordem_apresentacao INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cadeira_id) REFERENCES cadeiras(id) ON DELETE CASCADE
);

-- 9. Membros dos Grupos
CREATE TABLE grupo_membros (
    grupo_id VARCHAR(36) NOT NULL,
    estudante_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (grupo_id, estudante_id),
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    FOREIGN KEY (estudante_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 10. Eventos (Testes, Apresentações)
CREATE TABLE eventos (
    id VARCHAR(36) PRIMARY KEY,
    cadeira_id VARCHAR(36) NOT NULL,
    grupo_id VARCHAR(36) NULL,
    tipo ENUM('TESTE', 'APRESENTACAO', 'OUTRO') NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    data_evento DATETIME NOT NULL,
    estado ENUM('PENDENTE', 'CONCLUIDO') DEFAULT 'PENDENTE',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cadeira_id) REFERENCES cadeiras(id) ON DELETE CASCADE,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE SET NULL
);

-- 11. Sessões de Aulas e Presenças
CREATE TABLE sessoes (
    id VARCHAR(36) PRIMARY KEY,
    cadeira_id VARCHAR(36) NOT NULL,
    data_sessao DATE NOT NULL,
    confirmada BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cadeira_id) REFERENCES cadeiras(id) ON DELETE CASCADE
);

CREATE TABLE presencas (
    sessao_id VARCHAR(36) NOT NULL,
    estudante_id VARCHAR(36) NOT NULL,
    presente BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sessao_id, estudante_id),
    FOREIGN KEY (sessao_id) REFERENCES sessoes(id) ON DELETE CASCADE,
    FOREIGN KEY (estudante_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 12. Ficheiros (Trabalhos/Apresentações)
CREATE TABLE ficheiros (
    id VARCHAR(36) PRIMARY KEY,
    grupo_id VARCHAR(36) NOT NULL,
    categoria ENUM('TRABALHO', 'APRESENTACAO') NOT NULL,
    caminho_local VARCHAR(255) NULL,
    link_externo TEXT NULL,
    enviado_por VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    FOREIGN KEY (enviado_por) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 13. Reuniões de Grupo
CREATE TABLE reunioes (
    id VARCHAR(36) PRIMARY KEY,
    grupo_id VARCHAR(36) NOT NULL,
    modo ENUM('ONLINE', 'PRESENCIAL') NOT NULL,
    link_ou_local VARCHAR(255) NOT NULL,
    data_hora DATETIME NOT NULL,
    criada_por VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    FOREIGN KEY (criada_por) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 14. Enquetes e Votações
CREATE TABLE enquetes (
    id VARCHAR(36) PRIMARY KEY,
    turma_id VARCHAR(36) NOT NULL,
    pergunta VARCHAR(255) NOT NULL,
    prazo DATETIME NOT NULL,
    criada_por VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (turma_id) REFERENCES turmas(id) ON DELETE CASCADE,
    FOREIGN KEY (criada_por) REFERENCES utilizadores(id) ON DELETE CASCADE
);

CREATE TABLE opcoes_enquete (
    id VARCHAR(36) PRIMARY KEY,
    enquete_id VARCHAR(36) NOT NULL,
    texto VARCHAR(150) NOT NULL,
    FOREIGN KEY (enquete_id) REFERENCES enquetes(id) ON DELETE CASCADE
);

CREATE TABLE votos (
    enquete_id VARCHAR(36) NOT NULL,
    estudante_id VARCHAR(36) NOT NULL,
    opcao_id VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (enquete_id, estudante_id),
    FOREIGN KEY (enquete_id) REFERENCES enquetes(id) ON DELETE CASCADE,
    FOREIGN KEY (estudante_id) REFERENCES utilizadores(id) ON DELETE CASCADE,
    FOREIGN KEY (opcao_id) REFERENCES opcoes_enquete(id) ON DELETE CASCADE
);

-- 15. Notificações
CREATE TABLE notificacoes (
    id VARCHAR(36) PRIMARY KEY,
    destinatario_id VARCHAR(36) NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    texto TEXT NOT NULL,
    tipo ENUM('ALTERACAO', 'URGENTE', 'LEMBRETE', 'ENQUETE') NOT NULL,
    lida BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (destinatario_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);