package com.transnacala.lory.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.remote.ApiClient;
import com.transnacala.lory.data.remote.ApiService;

import java.util.List;

import retrofit2.Response;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        ApiService api = ApiClient.getClient().create(ApiService.class);

        // Fetch registros pendentes de envio
        List<EventoEntity> pendentes = db.eventoDao().getEventosPendentes();

        if (!pendentes.isEmpty()) {
            try {
                // Enviar para a API Python
                Response<Void> response = api.sincronizarEventos(pendentes).execute();
                if (response.isSuccessful()) {
                    // Atualizar estado local para SYNCED
                    for (EventoEntity e : pendentes) {
                        e.syncStatus = "SYNCED";
                        db.eventoDao().update(e);
                    }
                } else {
                    return Result.retry();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Result.retry();
            }
        }

        return Result.success();
    }
}