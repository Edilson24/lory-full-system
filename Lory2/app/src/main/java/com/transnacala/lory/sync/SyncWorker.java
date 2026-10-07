package com.transnacala.lory.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.remote.ApiClient;
import com.transnacala.lory.data.remote.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class SyncWorker extends Worker {

    private static final String TAG = "LorySyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        ApiService api = ApiClient.getApiService(getApplicationContext());

        List<String> cadeirasAceites = new ArrayList<>();
        List<String> eventosAceites = new ArrayList<>();
        List<String> enquetesAceites = new ArrayList<>();
        List<String> gruposAceites = new ArrayList<>();

        boolean success = pushCadeiras(api, db, cadeirasAceites);
        success = pushEventos(api, db, eventosAceites) && success;
        success = pushEnquetes(api, db, enquetesAceites) && success;
        success = pushGrupos(api, db, gruposAceites) && success;

        success = pullCadeiras(api, db, cadeirasAceites) && success;
        success = pullEventos(api, db, eventosAceites) && success;
        success = pullEnquetes(api, db, enquetesAceites) && success;
        success = pullGrupos(api, db, gruposAceites) && success;

        return success ? Result.success() : Result.retry();
    }

    private boolean pushCadeiras(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        List<CadeiraEntity> pending = db.cadeiraDao().getCadeirasPendentes();
        if (pending.isEmpty()) {
            return true;
        }
        try {
            Response<Map<String, Object>> response = api.sincronizarCadeiras(pending).execute();
            if (response.isSuccessful()) {
                for (CadeiraEntity cadeira : pending) {
                    acknowledgedIds.add(cadeira.id);
                }
                return true;
            }
            Log.w(TAG, "Push de cadeiras falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao enviar cadeiras pendentes.", exception);
        }
        return false;
    }

    private boolean pushEventos(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        List<EventoEntity> pending = db.eventoDao().getEventosPendentes();
        if (pending.isEmpty()) {
            return true;
        }
        try {
            Response<Map<String, Object>> response = api.sincronizarEventos(pending).execute();
            if (response.isSuccessful()) {
                for (EventoEntity evento : pending) {
                    acknowledgedIds.add(evento.id);
                }
                return true;
            }
            Log.w(TAG, "Push de eventos falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao enviar eventos pendentes.", exception);
        }
        return false;
    }

    private boolean pushEnquetes(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        List<EnqueteEntity> pending = db.enqueteDao().getEnquetesPendentes();
        if (pending.isEmpty()) {
            return true;
        }
        try {
            Response<Map<String, Object>> response = api.sincronizarEnquetes(pending).execute();
            if (response.isSuccessful()) {
                for (EnqueteEntity enquete : pending) {
                    acknowledgedIds.add(enquete.id);
                }
                return true;
            }
            Log.w(TAG, "Push de enquetes falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao enviar enquetes pendentes.", exception);
        }
        return false;
    }

    private boolean pushGrupos(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        List<GrupoEntity> pending = db.grupoDao().getGruposPendentes();
        if (pending.isEmpty()) {
            return true;
        }
        try {
            Response<Map<String, Object>> response = api.sincronizarGrupos(pending).execute();
            if (response.isSuccessful()) {
                for (GrupoEntity grupo : pending) {
                    acknowledgedIds.add(grupo.id);
                }
                return true;
            }
            Log.w(TAG, "Push de grupos falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao enviar grupos pendentes.", exception);
        }
        return false;
    }

    private boolean pullCadeiras(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        try {
            Response<List<CadeiraEntity>> response = api.getCadeirasServidor().execute();
            if (response.isSuccessful() && response.body() != null) {
                db.cadeiraDao().reconcileWithServer(response.body(), acknowledgedIds);
                return true;
            }
            Log.w(TAG, "Pull de cadeiras falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao obter cadeiras do servidor.", exception);
        }
        return false;
    }

    private boolean pullEventos(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        try {
            Response<List<EventoEntity>> response = api.getEventosServidor().execute();
            if (response.isSuccessful() && response.body() != null) {
                db.eventoDao().reconcileWithServer(response.body(), acknowledgedIds);
                return true;
            }
            Log.w(TAG, "Pull de eventos falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao obter eventos do servidor.", exception);
        }
        return false;
    }

    private boolean pullEnquetes(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        try {
            Response<List<EnqueteEntity>> response = api.getEnquetesServidor().execute();
            if (response.isSuccessful() && response.body() != null) {
                db.enqueteDao().reconcileWithServer(response.body(), acknowledgedIds);
                return true;
            }
            Log.w(TAG, "Pull de enquetes falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao obter enquetes do servidor.", exception);
        }
        return false;
    }

    private boolean pullGrupos(ApiService api, AppDatabase db, List<String> acknowledgedIds) {
        try {
            Response<List<GrupoEntity>> response = api.getGruposServidor().execute();
            if (response.isSuccessful() && response.body() != null) {
                db.grupoDao().reconcileWithServer(response.body(), acknowledgedIds);
                return true;
            }
            Log.w(TAG, "Pull de grupos falhou: HTTP " + response.code());
        } catch (Exception exception) {
            Log.e(TAG, "Falha ao obter grupos do servidor.", exception);
        }
        return false;
    }
}