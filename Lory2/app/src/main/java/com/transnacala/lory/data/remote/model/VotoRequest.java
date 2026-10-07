package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VotoRequest {
    @SerializedName("opcao_id")
    public final String opcaoId;
    @SerializedName("votado_em")
    public final String votadoEm;

    public VotoRequest(String opcaoId, long votedAt) {
        this.opcaoId = opcaoId;
        this.votadoEm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date(votedAt));
    }
}
