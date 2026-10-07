package com.transnacala.lory.data.remote.model;

public class PasswordRecoveryVerifyRequest {
    public final String email;
    public final String pin;

    public PasswordRecoveryVerifyRequest(String email, String pin) {
        this.email = email;
        this.pin = pin;
    }
}
