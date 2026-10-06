package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    public String nome;
    public String email;
    public String senha;

    @SerializedName("turma_id")
    public String turmaId;

    @SerializedName("data_nascimento")
    public String dataNascimento;

    public RegisterRequest(String nome, String email, String senha, String turmaId, String dataNascimento) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.turmaId = turmaId;
        this.dataNascimento = dataNascimento;
    }
}