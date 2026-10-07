package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "votos_locais", primaryKeys = {"questaoId", "estudanteId"})
public class VotoLocalEntity {
    @NonNull
    public String questaoId;
    @NonNull
    public String estudanteId;
    @NonNull
    public String opcaoId;
    @NonNull
    public String syncStatus;
    public long votedAt;

    public VotoLocalEntity(@NonNull String questaoId, @NonNull String estudanteId,
                           @NonNull String opcaoId, @NonNull String syncStatus, long votedAt) {
        this.questaoId = questaoId;
        this.estudanteId = estudanteId;
        this.opcaoId = opcaoId;
        this.syncStatus = syncStatus;
        this.votedAt = votedAt;
    }
}
