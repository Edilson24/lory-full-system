package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    public String id;
    public String nome;
    public String email;
    public String papel; // CHEFE, ESTUDANTE

    @SerializedName("turma_id")
    public String turmaId;
}