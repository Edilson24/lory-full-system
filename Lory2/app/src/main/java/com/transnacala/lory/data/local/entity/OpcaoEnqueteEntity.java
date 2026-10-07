package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "opcoes_enquete")
public class OpcaoEnqueteEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @SerializedName(value = "enqueteId", alternate = {"enquete_id"})
    public String enqueteId;
    @NonNull
    @SerializedName("questaoId")
    public String questaoId;
    public String texto;
    @SerializedName(value = "votosCount", alternate = {"votos_count"})
    public int votosCount;

    public OpcaoEnqueteEntity(@NonNull String id, String enqueteId, @NonNull String questaoId,
                              String texto, int votosCount) {
        this.id = id;
        this.enqueteId = enqueteId;
        this.questaoId = questaoId;
        this.texto = texto;
        this.votosCount = votosCount;
    }
}