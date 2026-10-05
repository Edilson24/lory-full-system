package com.transnacala.lory.data.remote;

import com.transnacala.lory.data.local.entity.EventoEntity;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/sync/eventos")
    Call<Void> sincronizarEventos(@Body List<EventoEntity> eventos);
}