package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

import com.transnacala.lory.data.remote.model.VotoEnqueteDto;

import java.util.List;

@Entity(tableName = "enquetes")
public class EnqueteEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @SerializedName(value = "turmaId", alternate = {"turma_id"})
    public String turmaId;
    public String pergunta;
    public String prazo;
    @SerializedName(value = "criadaPor", alternate = {"criada_por"})
    public String criadaPor;

    // Sincronização
    public String syncStatus;
    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public long updatedAt;
    @SerializedName("meusVotos")
    @Ignore
    public List<VotoEnqueteDto> meusVotos;
    @SerializedName("questoes")
    @Ignore
    public List<QuestaoEnqueteEntity> questoes;

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