from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
import mysql.connector
import jwt
import hashlib
import hmac
import logging
import os
import secrets
import smtplib
import uuid
from email.message import EmailMessage
from datetime import datetime, timedelta
from typing import List, Optional
from fastapi import BackgroundTasks

app = FastAPI(title="LORY API Backend", version="2.0")
security = HTTPBearer()
logger = logging.getLogger("lory.auth")

SECRET_KEY = "LORY_SUPER_SECRET_KEY_PRODUCTION_REPLACE_ME"
ALGORITHM = "HS256"


# Conexão com o Banco MySQL Local
def get_db():
    connection = mysql.connector.connect(
        host="localhost",
        user="root",
        password="root",  # Insira a sua senha do MySQL Workbench aqui
        database="lory_db",
        port=3306
    )
    try:
        yield connection
    finally:
        connection.close()


def hash_password(password: str, salt: str) -> str:
    return hashlib.sha256((password + salt).encode('utf-8')).hexdigest()


# ============================================================================
# SCHEMAS PYDANTIC (Aceitam camelCase enviado pelo Android/Retrofit)
# ============================================================================

class LoginSchema(BaseModel):
    email: str
    senha: str


class RegisterSchema(BaseModel):
    nome: str
    email: str
    senha: str
    turma_id: str
    data_nascimento: Optional[str] = None


class GroupMembersSchema(BaseModel):
    estudanteIds: List[str] = Field(..., alias="estudante_ids")

    class Config:
        populate_by_name = True


class PasswordRecoveryRequestSchema(BaseModel):
    email: str


class PasswordRecoveryVerifySchema(BaseModel):
    email: str
    pin: str


class PasswordRecoveryResetSchema(BaseModel):
    email: str
    reset_token: str
    nova_senha: str


class CadeiraSyncSchema(BaseModel):
    id: str
    turmaId: str = Field(..., alias="turma_id")
    semestreId: str = Field(..., alias="semestre_id")
    docenteId: str = Field(..., alias="docente_id")
    nome: str
    concluida: bool = False
    updatedAt: int = Field(..., alias="updated_at")

    class Config:
        populate_by_name = True


class EventoSyncSchema(BaseModel):
    id: str
    cadeiraId: str = Field(..., alias="cadeira_id")
    grupoId: Optional[str] = Field(None, alias="grupo_id")
    tipo: str
    titulo: str
    dataEvento: str = Field(..., alias="data_evento")
    estado: str
    updatedAt: int = Field(..., alias="updated_at")

    class Config:
        populate_by_name = True


class OpcaoEnqueteSyncSchema(BaseModel):
    id: str
    enqueteId: str = Field(..., alias="enquete_id")
    questaoId: str = Field(..., alias="questao_id")
    texto: str
    votosCount: int = Field(0, alias="votos_count")

    class Config:
        populate_by_name = True


class QuestaoEnqueteSyncSchema(BaseModel):
    id: str
    enqueteId: str = Field(..., alias="enquete_id")
    pergunta: str
    updatedAt: int = Field(..., alias="updated_at")
    opcoes: List[OpcaoEnqueteSyncSchema] = Field(default_factory=list)

    class Config:
        populate_by_name = True


class EnqueteSyncSchema(BaseModel):
    id: str
    turmaId: Optional[str] = Field(None, alias="turma_id")
    pergunta: str
    prazo: str
    criadaPor: Optional[str] = Field(None, alias="criada_por")
    updatedAt: int = Field(..., alias="updated_at")
    questoes: List[QuestaoEnqueteSyncSchema] = Field(default_factory=list)

    class Config:
        populate_by_name = True


# ============================================================================
# ROTAS / ENDPOINTS
# ============================================================================

@app.post("/api/v1/sync/cadeiras")
def sync_cadeiras(
        cadeiras_locais: List[CadeiraSyncSchema],
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    turma_id = payload.get("turma_id")
    if not turma_id or payload.get("papel") != "CHEFE":
        raise HTTPException(status_code=403, detail="Apenas o Chefe de Turma pode sincronizar cadeiras.")

    cursor = db.cursor(dictionary=True)
    sincronizadas = []

    for cadeira in cadeiras_locais:
        if cadeira.turmaId != turma_id:
            raise HTTPException(status_code=403, detail="A cadeira pertence a outra turma.")

        cursor.execute(
            "SELECT id FROM semestres WHERE id = %s AND turma_id = %s",
            (cadeira.semestreId, turma_id)
        )
        if not cursor.fetchone():
            raise HTTPException(status_code=422, detail=f"Semestre inválido para a cadeira {cadeira.id}.")

        cursor.execute(
            "SELECT id FROM docentes WHERE id = %s AND turma_id = %s",
            (cadeira.docenteId, turma_id)
        )
        if not cursor.fetchone():
            raise HTTPException(status_code=422, detail=f"Docente inválido para a cadeira {cadeira.id}.")

        cursor.execute("SELECT updated_at FROM cadeiras WHERE id = %s AND turma_id = %s", (cadeira.id, turma_id))
        existente = cursor.fetchone()
        data_atualizacao = datetime.utcfromtimestamp(cadeira.updatedAt / 1000)

        if not existente:
            cursor.execute(
                """INSERT INTO cadeiras
                   (id, turma_id, semestre_id, docente_id, nome, concluida, updated_at)
                   VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (cadeira.id, turma_id, cadeira.semestreId, cadeira.docenteId,
                 cadeira.nome, cadeira.concluida, data_atualizacao)
            )
            sincronizadas.append(cadeira.id)
        else:
            updated_at = existente["updated_at"]
            timestamp_servidor = int(updated_at.timestamp() * 1000) if updated_at else 0
            if cadeira.updatedAt > timestamp_servidor:
                cursor.execute(
                    """UPDATE cadeiras
                       SET semestre_id = %s, docente_id = %s, nome = %s, concluida = %s, updated_at = %s
                       WHERE id = %s AND turma_id = %s""",
                    (cadeira.semestreId, cadeira.docenteId, cadeira.nome, cadeira.concluida,
                     data_atualizacao, cadeira.id, turma_id)
                )
            sincronizadas.append(cadeira.id)

    db.commit()
    return {"status": "success", "synced_ids": sincronizadas}


@app.post("/api/v1/auth/login")
def login(credentials: LoginSchema, db=Depends(get_db)):
    cursor = db.cursor(dictionary=True)

    # 1. Buscar utilizador pelo email enviado (removendo espaços acidentais)
    email_limpo = credentials.email.strip()
    cursor.execute("SELECT * FROM utilizadores WHERE LOWER(email) = LOWER(%s)", (email_limpo,))
    user = cursor.fetchone()

    if not user:
        raise HTTPException(status_code=400, detail="E-mail ou senha incorretos.")

    # 2. Recalcular o Hash com a senha enviada e o salt armazenado no banco
    senha_enviada = credentials.senha.strip()
    hashed_enviado = hash_password(senha_enviada, user["salt"])

    # Se o hash for diferente (comparação case-insensitive)
    if hashed_enviado.lower() != user["senha_hash"].lower():
        raise HTTPException(status_code=400, detail="E-mail ou senha incorretos.")

    # 3. Validar estado do utilizador
    if user["estado"] == "PENDENTE":
        raise HTTPException(status_code=403, detail="Aguarde a confirmação do chefe")
    if user["estado"] == "REJEITADO":
        raise HTTPException(status_code=403, detail="Utilizador rejeitado pelo chefe")

    # 4. Gerar Token JWT de 30 dias
    expira = datetime.utcnow() + timedelta(days=30)
    token_payload = {
        "sub": user["id"],
        "turma_id": user["turma_id"],
        "papel": user["papel"],
        "exp": expira
    }
    token = jwt.encode(token_payload, SECRET_KEY, algorithm=ALGORITHM)

    return {
        "access_token": token,
        "token_type": "bearer",
        "user": {
            "id": user["id"],
            "nome": user["nome"],
            "email": user["email"],
            "papel": user["papel"],
            "turma_id": user["turma_id"]
        }
    }


@app.post("/api/v1/auth/register", status_code=201)
def register(credentials: RegisterSchema, db=Depends(get_db)):
    nome = credentials.nome.strip()
    email = credentials.email.strip().lower()
    senha = credentials.senha
    turma_id = credentials.turma_id.strip()
    if not nome or len(nome) > 120 or "@" not in email or len(email) > 150:
        raise HTTPException(status_code=422, detail="Nome ou e-mail inválido.")
    if len(senha) < 8:
        raise HTTPException(status_code=422, detail="A palavra-passe deve ter pelo menos 8 caracteres.")

    cursor = db.cursor(dictionary=True)
    cursor.execute("SELECT id FROM turmas WHERE id = %s", (turma_id,))
    if not cursor.fetchone():
        raise HTTPException(status_code=404, detail="Turma não encontrada.")
    cursor.execute("SELECT id FROM utilizadores WHERE LOWER(email) = LOWER(%s)", (email,))
    if cursor.fetchone():
        raise HTTPException(status_code=409, detail="Já existe uma conta com este e-mail.")

    user_id = str(uuid.uuid4())
    salt = secrets.token_hex(32)
    cursor.execute(
        """INSERT INTO utilizadores
           (id, turma_id, nome, email, senha_hash, salt, papel, estado, data_nascimento)
           VALUES (%s, %s, %s, %s, %s, %s, 'ESTUDANTE', 'PENDENTE', %s)""",
        (user_id, turma_id, nome, email, hash_password(senha, salt), salt, credentials.data_nascimento)
    )
    db.commit()
    return {"status": "success", "message": "Registo efetuado. Aguarde a aprovação do chefe."}


def _password_reset_secret() -> bytes:
    secret = os.getenv("PASSWORD_RESET_SECRET")
    if not secret:
        raise HTTPException(status_code=503, detail="Recuperação de senha não está configurada.")
    return secret.encode("utf-8")


def _reset_code_hash(pin: str) -> str:
    return hmac.new(_password_reset_secret(), pin.encode("ascii"), hashlib.sha256).hexdigest()


def _send_password_reset_email(email: str, pin: str) -> None:
    host = os.getenv("SMTP_HOST")
    port = int(os.getenv("SMTP_PORT", "587"))
    username = os.getenv("SMTP_USER")
    password = os.getenv("SMTP_PASSWORD")
    sender = os.getenv("SMTP_FROM")
    starttls = os.getenv("SMTP_STARTTLS", "true").strip().lower() in ("1", "true", "yes")
    if not host or not username or not password or not sender:
        logger.error("SMTP password recovery is not configured.")
        return

    message = EmailMessage()
    message["Subject"] = "Código para recuperar a sua senha"
    message["From"] = sender
    message["To"] = email
    message.set_content(
        f"O seu código de recuperação é {pin}. "
        "É válido durante 7 minutos. Se não pediu esta alteração, ignore esta mensagem."
    )
    try:
        with smtplib.SMTP(host, port, timeout=15) as smtp:
            smtp.ehlo()
            if starttls:
                smtp.starttls()
                smtp.ehlo()
            smtp.login(username, password)
            smtp.send_message(message)
    except (OSError, smtplib.SMTPException):
        logger.exception("Could not send password recovery email.")


def _validate_smtp_config() -> None:
    if not all(os.getenv(key) for key in ("SMTP_HOST", "SMTP_USER", "SMTP_PASSWORD", "SMTP_FROM")):
        raise HTTPException(status_code=503, detail="O serviço de e-mail ainda não está configurado.")
    try:
        port = int(os.getenv("SMTP_PORT", "587"))
    except ValueError:
        raise HTTPException(status_code=503, detail="A porta SMTP está inválida.")
    if port < 1 or port > 65535:
        raise HTTPException(status_code=503, detail="A porta SMTP está inválida.")


@app.post("/api/v1/auth/password/recovery")
def request_password_recovery(
        request: PasswordRecoveryRequestSchema,
        background_tasks: BackgroundTasks,
        db=Depends(get_db)
):
    _password_reset_secret()
    _validate_smtp_config()
    email = request.email.strip().lower()
    generic_response = {
        "status": "success",
        "message": "Se a conta existir, será enviado um código para o e-mail informado."
    }
    cursor = db.cursor(dictionary=True)
    cursor.execute("SELECT id, email FROM utilizadores WHERE LOWER(email) = LOWER(%s)", (email,))
    user = cursor.fetchone()
    if not user:
        return generic_response

    cursor.execute(
        """SELECT id FROM password_reset_tokens
           WHERE user_id = %s AND created_at > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 60 SECOND)
             AND used_at IS NULL LIMIT 1""",
        (user["id"],)
    )
    if cursor.fetchone():
        return generic_response

    pin = f"{secrets.randbelow(1_000_000):06d}"
    reset_id = str(uuid.uuid4())
    cursor.execute(
        """UPDATE password_reset_tokens SET used_at = UTC_TIMESTAMP()
           WHERE user_id = %s AND used_at IS NULL""",
        (user["id"],)
    )
    cursor.execute(
        """INSERT INTO password_reset_tokens
           (id, user_id, pin_hash, expires_at, attempts, created_at)
           VALUES (%s, %s, %s, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 MINUTE), 0, UTC_TIMESTAMP())""",
        (reset_id, user["id"], _reset_code_hash(pin))
    )
    db.commit()
    background_tasks.add_task(_send_password_reset_email, user["email"], pin)
    return generic_response


@app.post("/api/v1/auth/password/verify")
def verify_password_recovery(request: PasswordRecoveryVerifySchema, db=Depends(get_db)):
    _password_reset_secret()
    if len(request.pin) != 6 or not request.pin.isdigit():
        raise HTTPException(status_code=400, detail="Código inválido ou expirado.")
    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT r.id, r.user_id, r.pin_hash
           FROM password_reset_tokens r
           JOIN utilizadores u ON u.id = r.user_id
           WHERE LOWER(u.email) = LOWER(%s) AND r.used_at IS NULL
             AND r.expires_at > UTC_TIMESTAMP() AND r.attempts < 5
           ORDER BY r.created_at DESC LIMIT 1""",
        (request.email.strip(),)
    )
    reset = cursor.fetchone()
    if not reset:
        raise HTTPException(status_code=400, detail="Código inválido ou expirado.")
    pin_hash = _reset_code_hash(request.pin)
    if not hmac.compare_digest(pin_hash, reset["pin_hash"]):
        cursor.execute(
            "UPDATE password_reset_tokens SET attempts = attempts + 1 WHERE id = %s",
            (reset["id"],)
        )
        db.commit()
        raise HTTPException(status_code=400, detail="Código inválido ou expirado.")

    reset_token = secrets.token_urlsafe(32)
    token_hash = hmac.new(_password_reset_secret(), reset_token.encode("ascii"), hashlib.sha256).hexdigest()
    cursor.execute(
        """UPDATE password_reset_tokens
           SET reset_token_hash = %s, verified_at = UTC_TIMESTAMP() WHERE id = %s""",
        (token_hash, reset["id"])
    )
    db.commit()
    return {"reset_token": reset_token}


@app.post("/api/v1/auth/password/reset")
def reset_password(request: PasswordRecoveryResetSchema, db=Depends(get_db)):
    if len(request.nova_senha) < 8:
        raise HTTPException(status_code=422, detail="A palavra-passe deve ter pelo menos 8 caracteres.")
    token_hash = hmac.new(
        _password_reset_secret(), request.reset_token.encode("ascii"), hashlib.sha256
    ).hexdigest()
    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT r.id, r.user_id FROM password_reset_tokens r
           JOIN utilizadores u ON u.id = r.user_id
           WHERE LOWER(u.email) = LOWER(%s) AND r.reset_token_hash = %s
             AND r.verified_at IS NOT NULL AND r.used_at IS NULL
             AND r.expires_at > UTC_TIMESTAMP()
           ORDER BY r.created_at DESC LIMIT 1""",
        (request.email.strip(), token_hash)
    )
    reset = cursor.fetchone()
    if not reset:
        raise HTTPException(status_code=400, detail="Sessão de recuperação inválida ou expirada.")

    salt = secrets.token_hex(32)
    cursor.execute(
        "UPDATE utilizadores SET senha_hash = %s, salt = %s WHERE id = %s",
        (hash_password(request.nova_senha, salt), salt, reset["user_id"])
    )
    cursor.execute(
        "UPDATE password_reset_tokens SET used_at = UTC_TIMESTAMP() WHERE id = %s",
        (reset["id"],)
    )
    cursor.execute(
        """UPDATE password_reset_tokens SET used_at = UTC_TIMESTAMP()
           WHERE user_id = %s AND id != %s AND used_at IS NULL""",
        (reset["user_id"], reset["id"])
    )
    db.commit()
    return {"status": "success", "message": "Senha redefinida com sucesso."}


@app.post("/api/v1/sync/eventos")
def sync_eventos(
        eventos_locais: List[EventoSyncSchema],
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)

    for ev in eventos_locais:
        data_atualizacao = datetime.utcfromtimestamp(ev.updatedAt / 1000)
        cursor.execute(
            "SELECT id FROM cadeiras WHERE id = %s AND turma_id = %s",
            (ev.cadeiraId, payload.get("turma_id"))
        )
        cad = cursor.fetchone()
        if not cad:
            raise HTTPException(status_code=422, detail=f"Cadeira inválida para o evento {ev.id}.")

        if ev.grupoId:
            cursor.execute(
                """SELECT id FROM grupos
                   WHERE id = %s AND cadeira_id = %s""",
                (ev.grupoId, ev.cadeiraId)
            )
            if not cursor.fetchone():
                raise HTTPException(status_code=422, detail=f"Grupo incompatível com a cadeira do evento {ev.id}.")

        cursor.execute(
            """SELECT e.updated_at FROM eventos e
               JOIN cadeiras c ON c.id = e.cadeira_id
               WHERE e.id = %s AND c.turma_id = %s""",
            (ev.id, payload.get("turma_id"))
        )
        existente = cursor.fetchone()

        if not existente:
            sql = """INSERT INTO eventos
                     (id, cadeira_id, grupo_id, tipo, titulo, data_evento, estado, updated_at)
                     VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"""
            cursor.execute(sql, (
                ev.id, ev.cadeiraId, ev.grupoId, ev.tipo, ev.titulo,
                ev.dataEvento, ev.estado, data_atualizacao
            ))
        else:
            if payload.get("papel") != "CHEFE":
                raise HTTPException(status_code=403, detail="Apenas o Chefe de Turma pode alterar eventos.")
            ts_servidor = int(existente["updated_at"].timestamp() * 1000) if existente["updated_at"] else 0
            if ev.updatedAt > ts_servidor:
                sql = """UPDATE eventos SET cadeira_id=%s, grupo_id=%s, tipo=%s, titulo=%s, data_evento=%s,
                         estado=%s, updated_at=%s
                         WHERE id=%s"""
                cursor.execute(sql, (
                    ev.cadeiraId, ev.grupoId, ev.tipo, ev.titulo,
                    ev.dataEvento, ev.estado, data_atualizacao, ev.id
                ))

    db.commit()
    return {"status": "success", "message": "Eventos sincronizados com sucesso."}


@app.delete("/api/v1/eventos/{evento_id}")
def delete_evento(
        evento_id: str,
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    if payload.get("papel") != "CHEFE":
        raise HTTPException(status_code=403, detail="Apenas o Chefe de Turma pode eliminar eventos.")

    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT e.id FROM eventos e
           JOIN cadeiras c ON c.id = e.cadeira_id
           WHERE e.id = %s AND c.turma_id = %s""",
        (evento_id, payload.get("turma_id"))
    )
    if cursor.fetchone():
        cursor.execute("DELETE FROM eventos WHERE id = %s", (evento_id,))
        db.commit()
    return {"status": "success", "deleted_id": evento_id}


@app.post("/api/v1/sync/enquetes")
def sync_enquetes(
        enquetes_locais: List[EnqueteSyncSchema],
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)

    for enq in enquetes_locais:
        if payload.get("papel") != "CHEFE":
            raise HTTPException(status_code=403, detail="Apenas o Chefe de Turma pode criar enquetes.")

        cursor.execute("SELECT id, turma_id FROM enquetes WHERE id = %s", (enq.id,))
        existente = cursor.fetchone()

        if not existente:
            cursor.execute(
                """INSERT INTO enquetes (id, turma_id, pergunta, prazo, criada_por)
                   VALUES (%s, %s, %s, %s, %s)""",
                (enq.id, payload.get("turma_id"), enq.pergunta, enq.prazo, payload.get("sub"))
            )
        elif existente["turma_id"] != payload.get("turma_id"):
            raise HTTPException(status_code=403, detail="A enquete pertence a outra turma.")

        if not enq.questoes:
            raise HTTPException(status_code=422, detail="A enquete deve ter pelo menos uma questão.")
        for questao in enq.questoes:
            if questao.enqueteId != enq.id or len(questao.opcoes) < 2:
                raise HTTPException(status_code=422, detail="Cada questão deve ter pelo menos duas opções.")
            cursor.execute(
                """INSERT IGNORE INTO questoes_enquete
                   (id, enquete_id, pergunta, updated_at)
                   VALUES (%s, %s, %s, %s)""",
                (questao.id, enq.id, questao.pergunta,
                 datetime.utcfromtimestamp(questao.updatedAt / 1000))
            )
            for opcao in questao.opcoes:
                if opcao.enqueteId != enq.id or opcao.questaoId != questao.id:
                    raise HTTPException(status_code=422, detail="Opção vinculada a uma questão incorreta.")
                cursor.execute(
                    """INSERT IGNORE INTO opcoes_enquete
                       (id, enquete_id, questao_id, texto)
                       VALUES (%s, %s, %s, %s)""",
                    (opcao.id, enq.id, questao.id, opcao.texto)
                )

    db.commit()
    return {"status": "success", "message": "Enquetes sincronizadas com sucesso."}


class VotoSchema(BaseModel):
    opcaoId: str = Field(..., alias="opcao_id")
    votadoEm: str = Field(..., alias="votado_em")

    class Config:
        populate_by_name = True


@app.post("/api/v1/questoes/{questao_id}/votos")
def votar_enquete(
        questao_id: str,
        voto: VotoSchema,
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT q.enquete_id AS enqueteId, e.turma_id, e.prazo
           FROM questoes_enquete q
           JOIN enquetes e ON e.id = q.enquete_id
           WHERE q.id = %s""",
        (questao_id,)
    )
    enquete = cursor.fetchone()
    if not enquete or enquete["turma_id"] != payload.get("turma_id"):
        raise HTTPException(status_code=404, detail="Enquete não encontrada para esta turma.")
    try:
        momento_voto = datetime.strptime(voto.votadoEm, "%Y-%m-%d %H:%M:%S")
    except ValueError:
        raise HTTPException(status_code=422, detail="Data do voto inválida.")
    if momento_voto > enquete["prazo"]:
        raise HTTPException(status_code=409, detail="O voto foi realizado após o prazo da enquete.")

    cursor.execute(
        "SELECT id FROM opcoes_enquete WHERE id = %s AND questao_id = %s",
        (voto.opcaoId, questao_id)
    )
    if not cursor.fetchone():
        raise HTTPException(status_code=422, detail="Opção inválida para esta enquete.")

    cursor.execute(
        """INSERT INTO votos (enquete_id, questao_id, estudante_id, opcao_id, updated_at)
           VALUES (%s, %s, %s, %s, %s)
           ON DUPLICATE KEY UPDATE opcao_id = VALUES(opcao_id), updated_at = VALUES(updated_at)""",
        (enquete["enqueteId"], questao_id, payload.get("sub"), voto.opcaoId, momento_voto)
    )
    db.commit()
    return {"status": "success", "questao_id": questao_id, "opcao_id": voto.opcaoId}

# ============================================================================
# SCHEMAS ADICIONAIS PARA GRUPOS
# ============================================================================

class GrupoSyncSchema(BaseModel):
    id: str
    cadeiraId: str = Field(..., alias="cadeira_id")
    nome: str
    tema: Optional[str] = None
    dataCriacao: Optional[str] = Field(None, alias="data_criacao")
    estado: Optional[str] = None
    updatedAt: int = Field(..., alias="updated_at")

    class Config:
        populate_by_name = True

# ============================================================================
# ROTAS PULL (SERVIDOR -> CLIENTE / ROOM)
# ============================================================================

@app.get("/api/v1/cadeiras")
def get_cadeiras(
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    user_id = payload.get("sub")
    turma_id = payload.get("turma_id")
    papel = payload.get("papel")

    if not turma_id:
        raise HTTPException(status_code=403, detail="Utilizador sem turma associada.")

    select_sql = """SELECT c.id, c.turma_id AS turmaId, c.semestre_id AS semestreId,
                           c.docente_id AS docenteId, d.nome AS docenteNome, c.nome, c.categoria, c.progresso,
                           c.concluida, c.updated_at AS updatedAt
                    FROM cadeiras c
                    JOIN docentes d ON d.id = c.docente_id"""

    if papel == "ESTUDANTE":
        cursor.execute(
            select_sql + """ JOIN inscricoes i ON i.cadeira_id = c.id
                              WHERE c.turma_id = %s AND i.estudante_id = %s
                                AND c.concluida = FALSE
                              ORDER BY c.nome""",
            (turma_id, user_id)
        )
    elif papel == "CHEFE":
        cursor.execute(
            select_sql + " WHERE c.turma_id = %s ORDER BY c.nome",
            (turma_id,)
        )
    else:
        raise HTTPException(status_code=403, detail="Papel sem acesso às cadeiras da turma.")

    cadeiras = cursor.fetchall()
    for cadeira in cadeiras:
        cadeira["concluida"] = bool(cadeira["concluida"])
        updated_at = cadeira.get("updatedAt")
        cadeira["updatedAt"] = int(updated_at.timestamp() * 1000) if updated_at else 0

    return cadeiras


@app.get("/api/v1/enquetes")
def get_enquetes(
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT e.id, e.turma_id AS turmaId, e.pergunta, e.prazo, u.nome AS criadaPor,
                  CAST(UNIX_TIMESTAMP(e.updated_at) * 1000 AS UNSIGNED) AS updatedAt
           FROM enquetes e
           JOIN utilizadores u ON u.id = e.criada_por
           WHERE e.turma_id = %s
           ORDER BY e.updated_at DESC""",
        (payload.get("turma_id"),)
    )
    enquetes = cursor.fetchall()
    for enquete in enquetes:
        enquete["prazo"] = enquete["prazo"].strftime("%Y-%m-%d %H:%M:%S")
        cursor.execute(
            """SELECT q.id, q.enquete_id AS enqueteId, q.pergunta,
                      CAST(UNIX_TIMESTAMP(q.updated_at) * 1000 AS UNSIGNED) AS updatedAt
               FROM questoes_enquete q
               WHERE q.enquete_id = %s
               ORDER BY q.updated_at, q.id""",
            (enquete["id"],)
        )
        questoes = cursor.fetchall()
        for questao in questoes:
            cursor.execute(
                """SELECT o.id, o.enquete_id AS enqueteId, o.questao_id AS questaoId,
                          o.texto, COUNT(v.estudante_id) AS votosCount
                   FROM opcoes_enquete o
                   LEFT JOIN votos v ON v.opcao_id = o.id
                   WHERE o.questao_id = %s
                   GROUP BY o.id, o.enquete_id, o.questao_id, o.texto
                   ORDER BY o.id""",
                (questao["id"],)
            )
            questao["opcoes"] = cursor.fetchall()
        enquete["questoes"] = questoes
        cursor.execute(
            "SELECT questao_id AS questaoId, opcao_id AS opcaoId FROM votos WHERE enquete_id = %s AND estudante_id = %s",
            (enquete["id"], payload.get("sub"))
        )
        enquete["meusVotos"] = cursor.fetchall()
    return enquetes


@app.get("/api/v1/eventos")
def get_eventos(
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT e.id, e.cadeira_id AS cadeiraId, e.grupo_id AS grupoId, e.tipo, e.titulo,
                  e.data_evento AS dataEvento, e.estado,
                  CAST(UNIX_TIMESTAMP(e.updated_at) * 1000 AS UNSIGNED) AS updatedAt
           FROM eventos e
           JOIN cadeiras c ON e.cadeira_id = c.id
           WHERE c.turma_id = %s
           ORDER BY e.data_evento""",
        (payload.get("turma_id"),)
    )
    return cursor.fetchall()


@app.get("/api/v1/grupos")
def get_grupos(
        cadeira_id: Optional[str] = None,
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    if cadeira_id:
        cursor.execute(
            """SELECT g.id, g.cadeira_id AS cadeiraId, g.nome, g.tema,
                      CAST(UNIX_TIMESTAMP(g.updated_at) * 1000 AS UNSIGNED) AS updatedAt
               FROM grupos g
               JOIN cadeiras c ON g.cadeira_id = c.id
               WHERE c.turma_id = %s AND g.cadeira_id = %s
               ORDER BY g.nome""",
            (payload.get("turma_id"), cadeira_id)
        )
    else:
        cursor.execute(
            """SELECT g.id, g.cadeira_id AS cadeiraId, g.nome, g.tema,
                      CAST(UNIX_TIMESTAMP(g.updated_at) * 1000 AS UNSIGNED) AS updatedAt
               FROM grupos g
               JOIN cadeiras c ON g.cadeira_id = c.id
               WHERE c.turma_id = %s
               ORDER BY g.nome""",
            (payload.get("turma_id"),)
        )
    return cursor.fetchall()


@app.get("/api/v1/grupos/{grupo_id}/membros")
def get_membros_grupo(
        grupo_id: str,
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT g.cadeira_id FROM grupos g
           JOIN cadeiras c ON c.id = g.cadeira_id
           WHERE g.id = %s AND c.turma_id = %s""",
        (grupo_id, payload.get("turma_id"))
    )
    group = cursor.fetchone()
    if not group:
        raise HTTPException(status_code=404, detail="Grupo não encontrado.")
    cursor.execute(
        """SELECT u.id, u.nome, gm.grupo_id AS grupoId
           FROM inscricoes i
           JOIN utilizadores u ON u.id = i.estudante_id
           LEFT JOIN grupo_membros gm
             ON gm.estudante_id = u.id AND gm.cadeira_id = i.cadeira_id
           WHERE i.cadeira_id = %s AND u.estado = 'APROVADO' AND u.papel = 'ESTUDANTE'
           ORDER BY u.nome""",
        (group["cadeira_id"],)
    )
    return cursor.fetchall()


@app.put("/api/v1/grupos/{grupo_id}/membros")
def update_membros_grupo(
        grupo_id: str,
        request: GroupMembersSchema,
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")
    if payload.get("papel") != "CHEFE":
        raise HTTPException(status_code=403, detail="Apenas o Chefe de Turma pode gerir os membros.")

    student_ids = list(dict.fromkeys(request.estudanteIds))
    cursor = db.cursor(dictionary=True)
    cursor.execute(
        """SELECT g.cadeira_id FROM grupos g
           JOIN cadeiras c ON c.id = g.cadeira_id
           WHERE g.id = %s AND c.turma_id = %s""",
        (grupo_id, payload.get("turma_id"))
    )
    group = cursor.fetchone()
    if not group:
        raise HTTPException(status_code=404, detail="Grupo não encontrado.")
    chair_id = group["cadeira_id"]

    if student_ids:
        placeholders = ",".join(["%s"] * len(student_ids))
        cursor.execute(
            f"""SELECT u.id FROM utilizadores u
                JOIN inscricoes i ON i.estudante_id = u.id
                WHERE i.cadeira_id = %s AND u.estado = 'APROVADO'
                  AND u.papel = 'ESTUDANTE' AND u.id IN ({placeholders})""",
            [chair_id] + student_ids
        )
        eligible_ids = {row["id"] for row in cursor.fetchall()}
        if eligible_ids != set(student_ids):
            raise HTTPException(status_code=422, detail="Um ou mais estudantes não estão inscritos nesta cadeira.")

        cursor.execute(
            f"""SELECT estudante_id FROM grupo_membros
                WHERE cadeira_id = %s AND grupo_id != %s
                  AND estudante_id IN ({placeholders})""",
            [chair_id, grupo_id] + student_ids
        )
        conflicts = [row["estudante_id"] for row in cursor.fetchall()]
        if conflicts:
            raise HTTPException(
                status_code=409,
                detail="Um ou mais estudantes já pertencem a outro grupo desta cadeira."
            )

    try:
        cursor.execute("DELETE FROM grupo_membros WHERE grupo_id = %s", (grupo_id,))
        for student_id in student_ids:
            cursor.execute(
                """INSERT INTO grupo_membros (grupo_id, cadeira_id, estudante_id)
                   VALUES (%s, %s, %s)""",
                (grupo_id, chair_id, student_id)
            )
        db.commit()
    except mysql.connector.IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=409,
            detail="A composição do grupo foi alterada em simultâneo. Atualize e tente novamente."
        )
    return {"status": "success", "grupo_id": grupo_id, "estudante_ids": student_ids}


@app.post("/api/v1/sync/grupos")
def sync_grupos(
        grupos_locais: List[GrupoSyncSchema],
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)

    for g in grupos_locais:
        cursor.execute(
            "SELECT id FROM cadeiras WHERE id = %s AND turma_id = %s",
            (g.cadeiraId, payload.get("turma_id"))
        )
        cad = cursor.fetchone()
        if not cad:
            raise HTTPException(status_code=422, detail=f"Cadeira inválida para o grupo {g.id}.")

        cursor.execute(
            """SELECT gr.updated_at FROM grupos gr
               JOIN cadeiras c ON c.id = gr.cadeira_id
               WHERE gr.id = %s AND c.turma_id = %s""",
            (g.id, payload.get("turma_id"))
        )
        existente = cursor.fetchone()
        data_atualizacao = datetime.utcfromtimestamp(g.updatedAt / 1000)

        if not existente:
            cursor.execute(
                """INSERT INTO grupos (id, cadeira_id, nome, tema, updated_at)
                   VALUES (%s, %s, %s, %s, %s)""",
                (g.id, g.cadeiraId, g.nome, g.tema, data_atualizacao)
            )
        else:
            updated_at = existente["updated_at"]
            timestamp_servidor = int(updated_at.timestamp() * 1000) if updated_at else 0
            if g.updatedAt > timestamp_servidor:
                cursor.execute(
                    """UPDATE grupos SET cadeira_id = %s, nome = %s, tema = %s, updated_at = %s
                       WHERE id = %s""",
                    (g.cadeiraId, g.nome, g.tema, data_atualizacao, g.id)
                )

    db.commit()
    return {"status": "success", "synced_ids": [grupo.id for grupo in grupos_locais]}