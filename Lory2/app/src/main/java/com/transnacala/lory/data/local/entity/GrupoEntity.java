package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grupos")
public class GrupoEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String cadeiraId;
    public String nome;
    public String tema;
    public String dataCriacao;
    public String estado; // Em Andamento, Concluído

    // Sincronização
    public String syncStatus;
    public long updatedAt;

    public GrupoEntity(@NonNull String id, String cadeiraId, String nome, String tema, String dataCriacao, String estado, String syncStatus, long updatedAt) {
        this.id = id;
        this.cadeiraId = cadeiraId;
        this.nome = nome;
        this.tema = tema;
        this.dataCriacao = dataCriacao;
        this.estado = estado;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}