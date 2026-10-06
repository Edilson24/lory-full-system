package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cadeiras")
public class CadeiraEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String turmaId;
    public String semestreId;
    public String docenteId;
    public String nome;
    public String categoria; // Ex: E-Commerce, Hotel, Marketing, Task Manager
    public int progresso; // 0 a 100
    public boolean concluida;

    // Sincronização
    public String syncStatus;
    public long updatedAt;

    public CadeiraEntity(@NonNull String id, String turmaId, String semestreId, String docenteId, String nome, String categoria, int progresso, boolean concluida, String syncStatus, long updatedAt) {
        this.id = id;
        this.turmaId = turmaId;
        this.semestreId = semestreId;
        this.docenteId = docenteId;
        this.nome = nome;
        this.categoria = categoria;
        this.progresso = progresso;
        this.concluida = concluida;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}