package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class GroupMemberDto {
    public String id;
    public String nome;
    @SerializedName("grupoId")
    public String grupoId;
}
