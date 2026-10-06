package com.transnacala.lory.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.lory.databinding.ActivityRegisterBinding;
import com.transnacala.lory.repository.AuthRepository;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        binding.btnRegister.setOnClickListener(v -> performRegister());

        binding.tvLoginLink.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String name = binding.etRegisterName.getText().toString().trim();
        String email = binding.etRegisterEmail.getText().toString().trim();
        String password = binding.etRegisterPassword.getText().toString().trim();
        String turmaId = binding.etRegisterTurma.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etRegisterName.setError("Informe o seu nome completo");
            binding.etRegisterName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            binding.etRegisterEmail.setError("Informe o seu e-mail");
            binding.etRegisterEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.etRegisterPassword.setError("Informe a sua palavra-passe");
            binding.etRegisterPassword.requestFocus();
            return;
        }

        if (turmaId.isEmpty()) {
            binding.etRegisterTurma.setError("Informe o ID da turma");
            binding.etRegisterTurma.requestFocus();
            return;
        }

        setLoading(true);

        authRepository.register(name, email, password, turmaId, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.registerProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        binding.etRegisterName.setEnabled(!isLoading);
        binding.etRegisterEmail.setEnabled(!isLoading);
        binding.etRegisterPassword.setEnabled(!isLoading);
        binding.etRegisterTurma.setEnabled(!isLoading);
    }
}