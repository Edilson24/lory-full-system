package com.transnacala.lory.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;
import com.transnacala.lory.data.remote.ApiClient;
import com.transnacala.lory.data.remote.ApiService;
import com.transnacala.lory.data.remote.model.LoginRequest;
import com.transnacala.lory.data.remote.model.LoginResponse;
import com.transnacala.lory.data.remote.model.RegisterRequest;
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

    public AuthRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.sessionManager = new SessionManager(context);
        this.db = AppDatabase.getInstance(context);
    }

    public void login(String email, String senha, AuthCallback callback) {
        LoginRequest request = new LoginRequest(email, senha);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    UserDto user = body.user;

                    if (user != null) {
                        // Save session
                        sessionManager.saveSession(
                                body.accessToken,
                                user.id,
                                user.nome,
                                user.email,
                                user.papel,
                                user.turmaId
                        );

                        // Save user in Room DB
                        Executors.newSingleThreadExecutor().execute(() -> {
                            UtilizadorEntity entity = new UtilizadorEntity(
                                    user.id,
                                    user.turmaId,
                                    user.nome,
                                    user.email,
                                    user.papel,
                                    "APROVADO"
                            );
                            db.utilizadorDao().insert(entity);
                        });

                        callback.onSuccess("Login efetuado com sucesso!");
                    } else {
                        callback.onError("Dados de utilizador inválidos.");
                    }
                } else {
                    String errorMsg = parseErrorMessage(response);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onError("Falha na conexão com o servidor. Verifique sua internet.");
            }
        });
    }

    public void register(String nome, String email, String senha, String turmaId, AuthCallback callback) {
        RegisterRequest request = new RegisterRequest(nome, email, senha, turmaId, null);
        apiService.register(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Registo efetuado com sucesso! Aguarde a confirmação do chefe.");
                } else {
                    String errorMsg = parseErrorMessage(response);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (response.code() == 400) return "Credenciais inválidas.";
        if (response.code() == 403) return "Acesso negado ou conta pendente de aprovação.";
        return "Erro no servidor (" + response.code() + ").";
    }
}