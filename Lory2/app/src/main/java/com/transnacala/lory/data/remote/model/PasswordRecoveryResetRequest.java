package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class PasswordRecoveryResetRequest {
    public final String email;
    @SerializedName("reset_token")
    public final String resetToken;
    @SerializedName("nova_senha")
    public final String novaSenha;

    public PasswordRecoveryResetRequest(String email, String resetToken, String novaSenha) {
        this.email = email;
        this.resetToken = resetToken;
        this.novaSenha = novaSenha;
    }
}
