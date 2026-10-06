package com.transnacala.lory.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.remote.ApiClient;
import com.transnacala.lory.data.remote.ApiService;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        ApiService api = ApiClient.getApiService(context);

        boolean allSuccess = true;

        // 1. Sync Eventos
        List<EventoEntity> eventosPendentes = db.eventoDao().getEventosPendentes();
        if (!eventosPendentes.isEmpty()) {
            try {
                Response<Map<String, Object>> response = api.sincronizarEventos(eventosPendentes).execute();
                if (response.isSuccessful()) {
                    for (EventoEntity e : eventosPendentes) {
                        e.syncStatus = Constants.SYNC_STATUS_SYNCED;
                        db.eventoDao().update(e);
                    }
                } else {
                    allSuccess = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                allSuccess = false;
            }
        }

        // 2. Sync Cadeiras
        List<CadeiraEntity> cadeirasPendentes = db.cadeiraDao().getCadeirasPendentes();
        if (!cadeirasPendentes.isEmpty()) {
            try {
                Response<Map<String, Object>> response = api.sincronizarCadeiras(cadeirasPendentes).execute();
                if (response.isSuccessful()) {
                    for (CadeiraEntity c : cadeirasPendentes) {
                        c.syncStatus = Constants.SYNC_STATUS_SYNCED;
                        db.cadeiraDao().update(c);
                    }
                } else {
                    allSuccess = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                allSuccess = false;
            }
        }

        // 3. Sync Enquetes
        List<EnqueteEntity> enquetesPendentes = db.enqueteDao().getEnquetesPendentes();
        if (!enquetesPendentes.isEmpty()) {
            try {
                Response<Map<String, Object>> response = api.sincronizarEnquetes(enquetesPendentes).execute();
                if (response.isSuccessful()) {
                    for (EnqueteEntity eq : enquetesPendentes) {
                        eq.syncStatus = Constants.SYNC_STATUS_SYNCED;
                        db.enqueteDao().update(eq);
                    }
                } else {
                    allSuccess = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                allSuccess = false;
            }
        }

        return allSuccess ? Result.success() : Result.retry();
    }
}