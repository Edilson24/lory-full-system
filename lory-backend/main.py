from fastapi import FastAPI, Depends, HTTPException, status, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
import mysql.connector
import jwt
import hashlib
import os
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
        password="root",  # Insira a sua senha do MySQL Workbench
        database="lory_db",
        port=3306
    )
    try:
        yield connection
    finally:
        connection.close()


# Helper de Criptografia de Senhas
def hash_password(password: str, salt: str) -> str:
    return hashlib.sha256((password + salt).encode('utf-8')).hexdigest()


# Modelo Pydantic para Login
class LoginSchema(BaseModel):
    email: str
    senha: str


class EventoSyncSchema(BaseModel):
    id: str
    cadeira_id: str
    grupo_id: Optional[str] = None
    tipo: str
    titulo: str
    data_evento: str
    estado: str
    updated_at: int


# Rotas de Autenticação
@app.post("/api/v1/auth/login")
def login(credentials: LoginSchema, db=Depends(get_db)):
    cursor = db.cursor(dictionary=True)
    cursor.execute("SELECT * FROM utilizadores WHERE email = %s", (credentials.email,))
    user = cursor.fetchone()

    if not user:
        raise HTTPException(status_code=400, detail="Credenciais inválidas.")

    hashed = hash_password(credentials.senha, user["salt"])
    if hashed != user["senha_hash"]:
        raise HTTPException(status_code=400, detail="Credenciais inválidas.")

    if user["estado"] == "PENDENTE":
        raise HTTPException(status_code=403, detail="Aguarde a confirmação do chefe [RN-02]")
    if user["estado"] == "REJEITADO":
        raise HTTPException(status_code=403, detail="Utilizador rejeitado pelo chefe [RN-03]")

    # Gera Token JWT
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


# Endpoint de Sincronização Bidirecional de Eventos
@app.post("/api/v1/sync/eventos")
def sync_eventos(
        eventos_locais: List[EventoSyncSchema],
        credentials: HTTPAuthorizationCredentials = Depends(security),
        db=Depends(get_db)
):
    # Validar JWT
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado.")

    cursor = db.cursor(dictionary=True)

    # 1. Processar dados vindos do Mobile (Cliente -> Servidor)
    for ev in eventos_locais:
        cursor.execute("SELECT updated_at FROM eventos WHERE id = %s", (ev.id,))
        existente = cursor.fetchone()

        if not existente:
            # Inserir novo
            sql = """INSERT INTO eventos (id, cadeira_id, grupo_id, tipo, titulo, data_evento, estado) 
                     VALUES (%s, %s, %s, %s, %s, %s, %s)"""
            cursor.execute(sql, (ev.id, ev.cadeira_id, ev.grupo_id, ev.tipo, ev.titulo, ev.data_evento, ev.estado))
        else:
            # Resolução de conflitos por "Last Write Wins" (Maior Timestamp)
            ts_servidor = int(existente["updated_at"].timestamp() * 1000) if existente["updated_at"] else 0
            if ev.updated_at > ts_servidor:
                sql = """UPDATE eventos SET cadeira_id=%s, grupo_id=%s, tipo=%s, titulo=%s, data_evento=%s, estado=%s 
                         WHERE id=%s"""
                cursor.execute(sql, (ev.cadeira_id, ev.grupo_id, ev.tipo, ev.titulo, ev.data_evento, ev.estado, ev.id))

    db.commit()

    # 2. Retornar todos os registros atualizados do banco para o Mobile (Servidor -> Cliente)
    cursor.execute("SELECT * FROM eventos WHERE cadeira_id IN (SELECT id FROM cadeiras WHERE turma_id = %s)",
                   (payload["turma_id"],))
    eventos_servidor = cursor.fetchall()

    return {"status": "success", "eventos": eventos_servidor}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)