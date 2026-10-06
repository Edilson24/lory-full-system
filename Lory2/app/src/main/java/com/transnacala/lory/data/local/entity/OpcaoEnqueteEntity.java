package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "opcoes_enquete")
public class OpcaoEnqueteEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String enqueteId;
    public String texto;
    public int votosCount;

    public OpcaoEnqueteEntity(@NonNull String id, String enqueteId, String texto, int votosCount) {
        this.id = id;
        this.enqueteId = enqueteId;
        this.texto = texto;
        this.votosCount = votosCount;
    }
}