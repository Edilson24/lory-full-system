package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enquetes")
public class EnqueteEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String turmaId;
    public String pergunta;
    public String prazo;
    public String criadaPor;

    // Sincronização
    public String syncStatus;
    public long updatedAt;

    public EnqueteEntity(@NonNull String id, String turmaId, String pergunta, String prazo, String criadaPor, String syncStatus, long updatedAt) {
        this.id = id;
        this.turmaId = turmaId;
        this.pergunta = pergunta;
        this.prazo = prazo;
        this.criadaPor = criadaPor;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}