package com.transnacala.lory.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;
import com.transnacala.lory.data.remote.ApiClient;
import com.transnacala.lory.data.remote.ApiService;
import com.transnacala.lory.data.remote.model.LoginRequest;
import com.transnacala.lory.data.remote.model.LoginResponse;
import com.transnacala.lory.data.remote.model.PasswordRecoveryRequest;
import com.transnacala.lory.data.remote.model.PasswordRecoveryResetRequest;
import com.transnacala.lory.data.remote.model.PasswordRecoveryVerifyRequest;
import com.transnacala.lory.data.remote.model.RegisterRequest;
import com.transnacala.lory.data.remote.model.ResetTokenResponse;
import com.transnacala.lory.data.remote.model.UserDto;
import com.transnacala.lory.utils.SessionManager;

import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;

    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface RecoveryVerifyCallback {
        void onSuccess(String resetToken);
        void onError(String errorMessage);
    }

    public AuthRepository(Context context) {
        apiService = ApiClient.getApiService(context);
        sessionManager = new SessionManager(context);
        db = AppDatabase.getInstance(context);
    }

    public void login(String email, String senha, AuthCallback callback) {
        apiService.login(new LoginRequest(email, senha)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    UserDto user = body.user;
                    if (user == null) {
                        callback.onError("Dados de utilizador inválidos.");
                        return;
                    }
                    sessionManager.saveSession(body.accessToken, user.id, user.nome,
                            user.email, user.papel, user.turmaId);
                    Executors.newSingleThreadExecutor().execute(() -> db.utilizadorDao().insert(
                            new UtilizadorEntity(user.id, user.turmaId, user.nome,
                                    user.email, user.papel, "APROVADO")));
                    callback.onSuccess("Login efetuado com sucesso!");
                } else {
                    callback.onError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable error) {
                callback.onError("Falha na conexão com o servidor. Verifique sua internet.");
            }
        });
    }

    public void register(String nome, String email, String senha, String turmaId, AuthCallback callback) {
        apiService.register(new RegisterRequest(nome, email, senha, turmaId, null))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess("Registo efetuado com sucesso. Aguarde a confirmação do chefe.");
                        } else {
                            callback.onError(parseErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                        callback.onError("Falha na conexão com o servidor.");
                    }
                });
    }

    public void requestPasswordRecovery(String email, AuthCallback callback) {
        apiService.requestPasswordRecovery(new PasswordRecoveryRequest(email))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess("Se a conta existir, será enviado um código para o e-mail informado.");
                        } else {
                            callback.onError(parseErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                        callback.onError("Falha na conexão com o servidor.");
                    }
                });
    }

    public void verifyPasswordRecovery(String email, String pin, RecoveryVerifyCallback callback) {
        apiService.verifyPasswordRecovery(new PasswordRecoveryVerifyRequest(email, pin))
                .enqueue(new Callback<ResetTokenResponse>() {
                    @Override
                    public void onResponse(Call<ResetTokenResponse> call,
                                           Response<ResetTokenResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().resetToken != null) {
                            callback.onSuccess(response.body().resetToken);
                        } else {
                            callback.onError(parseErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ResetTokenResponse> call, Throwable error) {
                        callback.onError("Falha na conexão com o servidor.");
                    }
                });
    }

    public void resetPassword(String email, String resetToken, String newPassword, AuthCallback callback) {
        apiService.resetPassword(new PasswordRecoveryResetRequest(email, resetToken, newPassword))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess("Senha redefinida. Já pode iniciar sessão.");
                        } else {
                            callback.onError(parseErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                        callback.onError("Falha na conexão com o servidor.");
                    }
                });
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                Map<?, ?> map = new Gson().fromJson(errorJson, Map.class);
                if (map != null && map.containsKey("detail")) {
                    return map.get("detail").toString();
                }
            }
        } catch (Exception exception) {
            return "Erro no servidor (" + response.code() + ").";
        }
        if (response.code() == 400) return "Credenciais inválidas.";
        if (response.code() == 403) return "Acesso negado ou conta pendente de aprovação.";
        return "Erro no servidor (" + response.code() + ").";
    }
}
