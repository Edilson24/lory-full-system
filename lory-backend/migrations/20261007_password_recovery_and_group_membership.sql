CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    pin_hash CHAR(64) NOT NULL,
    reset_token_hash CHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    verified_at DATETIME NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_password_reset_user_created (user_id, created_at),
    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

ALTER TABLE grupo_membros
    ADD COLUMN cadeira_id VARCHAR(36) NULL;

UPDATE grupo_membros gm
JOIN grupos g ON g.id = gm.grupo_id
SET gm.cadeira_id = g.cadeira_id
WHERE gm.cadeira_id IS NULL;

ALTER TABLE grupo_membros
    MODIFY cadeira_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_grupo_membro_cadeira
        FOREIGN KEY (cadeira_id) REFERENCES cadeiras(id) ON DELETE CASCADE,
    ADD CONSTRAINT uq_grupo_membro_cadeira_estudante
        UNIQUE (cadeira_id, estudante_id);

CREATE TABLE questoes_enquete (
    id VARCHAR(64) PRIMARY KEY,
    enquete_id VARCHAR(36) NOT NULL,
    pergunta VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (enquete_id) REFERENCES enquetes(id) ON DELETE CASCADE
);

INSERT INTO questoes_enquete (id, enquete_id, pergunta)
SELECT CONCAT('legacy-question-', id), id, pergunta FROM enquetes;

ALTER TABLE opcoes_enquete
    ADD COLUMN questao_id VARCHAR(64) NULL;

UPDATE opcoes_enquete
SET questao_id = CONCAT('legacy-question-', enquete_id);

ALTER TABLE opcoes_enquete
    MODIFY questao_id VARCHAR(64) NOT NULL,
    ADD CONSTRAINT fk_opcao_questao
        FOREIGN KEY (questao_id) REFERENCES questoes_enquete(id) ON DELETE CASCADE;

ALTER TABLE votos
    ADD COLUMN questao_id VARCHAR(64) NULL;

UPDATE votos v
JOIN opcoes_enquete o ON o.id = v.opcao_id
SET v.questao_id = o.questao_id;

ALTER TABLE votos
    DROP PRIMARY KEY,
    MODIFY questao_id VARCHAR(64) NOT NULL,
    ADD PRIMARY KEY (questao_id, estudante_id),
    ADD CONSTRAINT fk_voto_questao
        FOREIGN KEY (questao_id) REFERENCES questoes_enquete(id) ON DELETE CASCADE;
