package com.transnacala.lory.sync;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "eventos")
public class EventoEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID gerado localmente

    public String cadeiraId;
    public String grupoId;
    public String tipo; // TESTE, APRESENTACAO, OUTRO
    public String titulo;
    public String dataEvento; // Formato ISO-8601
    public String estado; // PENDENTE, CONCLUIDO

    // Controle de Sincronização
    public String syncStatus; // PENDING_INSERT, PENDING_UPDATE, SYNCED
    public long updatedAt;

    public EventoEntity(@NonNull String id, String cadeiraId, String grupoId, String tipo, String titulo, String dataEvento, String estado, String syncStatus, long updatedAt) {
        this.id = id;
        this.cadeiraId = cadeiraId;
        this.grupoId = grupoId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.dataEvento = dataEvento;
        this.estado = estado;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}