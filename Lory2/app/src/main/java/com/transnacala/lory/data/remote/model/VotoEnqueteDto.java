package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class VotoEnqueteDto {
    @SerializedName("questaoId")
    public String questaoId;
    @SerializedName("opcaoId")
    public String opcaoId;
}
