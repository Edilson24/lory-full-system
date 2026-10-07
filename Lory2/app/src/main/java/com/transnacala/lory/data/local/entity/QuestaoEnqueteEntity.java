package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "questoes_enquete")
public class QuestaoEnqueteEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String enqueteId;
    public String pergunta;
    public long updatedAt;
    @Ignore
    public List<OpcaoEnqueteEntity> opcoes;

    public QuestaoEnqueteEntity(@NonNull String id, String enqueteId, String pergunta, long updatedAt) {
        this.id = id;
        this.enqueteId = enqueteId;
        this.pergunta = pergunta;
        this.updatedAt = updatedAt;
    }
}
