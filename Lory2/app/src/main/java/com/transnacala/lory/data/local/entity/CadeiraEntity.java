package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "cadeiras")
public class CadeiraEntity {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @SerializedName(value = "turmaId", alternate = {"turma_id"})
    public String turmaId;

    @SerializedName(value = "semestreId", alternate = {"semestre_id"})
    public String semestreId;

    @SerializedName(value = "docenteId", alternate = {"docente_id"})
    public String docenteId;

    @SerializedName("nome")
    public String nome;

    @SerializedName("docenteNome")
    public String docenteNome;

    @SerializedName("categoria")
    public String categoria;

    @SerializedName("progresso")
    public int progresso;

    @SerializedName("concluida")
    public boolean concluida;

    public String syncStatus;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public long updatedAt;

    public CadeiraEntity(@NonNull String id, String turmaId, String semestreId, String docenteId, String nome, String categoria, int progresso, boolean concluida, String syncStatus, long updatedAt) {
        this.id = id;
        this.turmaId = turmaId;
        this.semestreId = semestreId;
        this.docenteId = docenteId;
        this.nome = nome;
        this.docenteNome = null;
        this.categoria = categoria;
        this.progresso = progresso;
        this.concluida = concluida;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}