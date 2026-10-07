package com.transnacala.lory.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.lory.databinding.ActivityPasswordRecoveryBinding;
import com.transnacala.lory.repository.AuthRepository;

public class PasswordRecoveryActivity extends AppCompatActivity {
    private ActivityPasswordRecoveryBinding binding;
    private AuthRepository authRepository;
    private String email;
    private String resetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordRecoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authRepository = new AuthRepository(this);

        binding.btnRequestPin.setOnClickListener(v -> requestPin());
        binding.btnVerifyPin.setOnClickListener(v -> verifyPin());
        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
        binding.tvRecoveryBackLogin.setOnClickListener(v -> finish());
    }

    private void requestPin() {
        email = binding.etRecoveryEmail.getText().toString().trim();
        if (email.isEmpty() || !email.contains("@")) {
            binding.etRecoveryEmail.setError("Informe um e-mail válido");
            return;
        }
        setLoading(true);
        authRepository.requestPasswordRecovery(email, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                binding.layoutRequestPin.setVisibility(View.GONE);
                binding.layoutVerifyPin.setVisibility(View.VISIBLE);
                Toast.makeText(PasswordRecoveryActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void verifyPin() {
        String pin = binding.etRecoveryPin.getText().toString().trim();
        if (pin.length() != 6) {
            binding.etRecoveryPin.setError("Informe os 6 dígitos do PIN");
            return;
        }
        setLoading(true);
        authRepository.verifyPasswordRecovery(email, pin, new AuthRepository.RecoveryVerifyCallback() {
            @Override
            public void onSuccess(String token) {
                setLoading(false);
                resetToken = token;
                binding.layoutVerifyPin.setVisibility(View.GONE);
                binding.layoutResetPassword.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void resetPassword() {
        String password = binding.etNewPassword.getText().toString();
        String confirmation = binding.etConfirmPassword.getText().toString();
        if (password.length() < 8) {
            binding.etNewPassword.setError("Use pelo menos 8 caracteres");
            return;
        }
        if (!password.equals(confirmation)) {
            binding.etConfirmPassword.setError("As palavras-passe não coincidem");
            return;
        }
        setLoading(true);
        authRepository.resetPassword(email, resetToken, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                Toast.makeText(PasswordRecoveryActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.recoveryProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRequestPin.setEnabled(!loading);
        binding.btnVerifyPin.setEnabled(!loading);
        binding.btnResetPassword.setEnabled(!loading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
