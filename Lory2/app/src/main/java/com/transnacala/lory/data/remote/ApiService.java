package com.transnacala.lory.data.remote;

import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.remote.model.LoginRequest;
import com.transnacala.lory.data.remote.model.LoginResponse;
import com.transnacala.lory.data.remote.model.RegisterRequest;
import com.transnacala.lory.data.remote.model.VotoRequest;
import com.transnacala.lory.data.remote.model.PasswordRecoveryRequest;
import com.transnacala.lory.data.remote.model.PasswordRecoveryVerifyRequest;
import com.transnacala.lory.data.remote.model.PasswordRecoveryResetRequest;
import com.transnacala.lory.data.remote.model.ResetTokenResponse;
import com.transnacala.lory.data.remote.model.GroupMemberDto;
import com.transnacala.lory.data.remote.model.GroupMembersRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest credentials);

    @POST("api/v1/auth/register")
    Call<Map<String, Object>> register(@Body RegisterRequest registerRequest);

    @POST("api/v1/auth/password/recovery")
    Call<Map<String, Object>> requestPasswordRecovery(@Body PasswordRecoveryRequest request);

    @POST("api/v1/auth/password/verify")
    Call<ResetTokenResponse> verifyPasswordRecovery(@Body PasswordRecoveryVerifyRequest request);

    @POST("api/v1/auth/password/reset")
    Call<Map<String, Object>> resetPassword(@Body PasswordRecoveryResetRequest request);

    // Push Sync (Client -> Server)
    @POST("api/v1/sync/eventos")
    Call<Map<String, Object>> sincronizarEventos(@Body List<EventoEntity> eventos);

    @DELETE("api/v1/eventos/{eventoId}")
    Call<Map<String, Object>> eliminarEvento(@Path("eventoId") String eventoId);

    @POST("api/v1/sync/cadeiras")
    Call<Map<String, Object>> sincronizarCadeiras(@Body List<CadeiraEntity> cadeiras);

    @POST("api/v1/sync/enquetes")
    Call<Map<String, Object>> sincronizarEnquetes(@Body List<EnqueteEntity> enquetes);

    @POST("api/v1/questoes/{questaoId}/votos")
    Call<Map<String, Object>> votar(@Path("questaoId") String questaoId, @Body VotoRequest voto);

    @POST("api/v1/sync/grupos")
    Call<Map<String, Object>> sincronizarGrupos(@Body List<GrupoEntity> grupos);

    @GET("api/v1/grupos/{grupoId}/membros")
    Call<List<GroupMemberDto>> getMembrosGrupo(@Path("grupoId") String grupoId);

    @PUT("api/v1/grupos/{grupoId}/membros")
    Call<Map<String, Object>> atualizarMembrosGrupo(
            @Path("grupoId") String grupoId, @Body GroupMembersRequest membros);

    // Pull Sync (Server -> Client)
    @GET("api/v1/cadeiras")
    Call<List<CadeiraEntity>> getCadeirasServidor();

    @GET("api/v1/enquetes")
    Call<List<EnqueteEntity>> getEnquetesServidor();

    @GET("api/v1/eventos")
    Call<List<EventoEntity>> getEventosServidor();

    @GET("api/v1/grupos")
    Call<List<GrupoEntity>> getGruposServidor();

    @GET("api/v1/grupos")
    Call<List<GrupoEntity>> getGruposDaCadeira(@Query("cadeira_id") String cadeiraId);

    @GET("api/v1/cadeiras")
    Call<List<CadeiraEntity>> getCadeiras();
}