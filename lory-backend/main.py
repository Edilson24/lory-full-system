from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
import mysql.connector
import jwt
import hashlib
from datetime import datetime, timedelta
from typing import List, Optional

app = FastAPI(title="LORY API Backend", version="2.0")
security = HTTPBearer()

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


class EnqueteSyncSchema(BaseModel):
    id: str
    turmaId: Optional[str] = Field(None, alias="turma_id")
    pergunta: str
    prazo: str
    criadaPor: Optional[str] = Field(None, alias="criada_por")
    updatedAt: int = Field(..., alias="updated_at")

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
            sql = """INSERT INTO eventos (id, cadeira_id, grupo_id, tipo, titulo, data_evento, estado)
                     VALUES (%s, %s, %s, %s, %s, %s, %s)"""
            cursor.execute(sql, (ev.id, ev.cadeiraId, ev.grupoId, ev.tipo, ev.titulo, ev.dataEvento, ev.estado))
        else:
            ts_servidor = int(existente["updated_at"].timestamp() * 1000) if existente["updated_at"] else 0
            if ev.updatedAt > ts_servidor:
                sql = """UPDATE eventos SET cadeira_id=%s, grupo_id=%s, tipo=%s, titulo=%s, data_evento=%s, estado=%s
                         WHERE id=%s"""
                cursor.execute(sql, (ev.cadeiraId, ev.grupoId, ev.tipo, ev.titulo, ev.dataEvento, ev.estado, ev.id))

    db.commit()
    return {"status": "success", "message": "Eventos sincronizados com sucesso."}


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
        cursor.execute("SELECT id FROM enquetes WHERE id = %s", (enq.id,))
        existente = cursor.fetchone()

        if not existente:
            sql = """INSERT INTO enquetes (id, turma_id, pergunta, prazo, criada_por) 
                     VALUES (%s, %s, %s, %s, %s)"""
            cursor.execute(sql, (enq.id, payload.get("turma_id"), enq.pergunta, enq.prazo, payload.get("sub")))

    db.commit()
    return {"status": "success", "message": "Enquetes sincronizadas com sucesso."}

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
                           c.docente_id AS docenteId, c.nome, c.categoria, c.progresso,
                           c.concluida, c.updated_at AS updatedAt
                    FROM cadeiras c"""

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
    cursor.execute("SELECT id, turma_id AS turmaId, pergunta, prazo, criada_por AS criadaPor FROM enquetes WHERE turma_id = %s", (payload.get("turma_id"),))
    return cursor.fetchall()


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
    cursor.execute("SELECT e.id, e.cadeira_id AS cadeiraId, e.grupo_id AS grupoId, e.tipo, e.titulo, e.data_evento AS dataEvento, e.estado FROM eventos e JOIN cadeiras c ON e.cadeira_id = c.id WHERE c.turma_id = %s", (payload.get("turma_id"),))
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