package com.transnacala.lory.data.remote;

import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.remote.model.LoginRequest;
import com.transnacala.lory.data.remote.model.LoginResponse;
import com.transnacala.lory.data.remote.model.RegisterRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<Map<String, Object>> register(@Body RegisterRequest registerRequest);

    @POST("api/v1/sync/eventos")
    Call<Map<String, Object>> sincronizarEventos(@Body List<EventoEntity> eventos);

    @POST("api/v1/sync/cadeiras")
    Call<Map<String, Object>> sincronizarCadeiras(@Body List<CadeiraEntity> cadeiras);

    @POST("api/v1/sync/enquetes")
    Call<Map<String, Object>> sincronizarEnquetes(@Body List<EnqueteEntity> enquetes);
}