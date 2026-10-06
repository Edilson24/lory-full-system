package com.transnacala.lory.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "utilizadores")
public class UtilizadorEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String turmaId;
    public String nome;
    public String email;
    public String papel; // CHEFE, ESTUDANTE
    public String estado; // APROVADO, PENDENTE, REJEITADO

    public UtilizadorEntity(@NonNull String id, String turmaId, String nome, String email, String papel, String estado) {
        this.id = id;
        this.turmaId = turmaId;
        this.nome = nome;
        this.email = email;
        this.papel = papel;
        this.estado = estado;
    }
}