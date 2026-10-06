package com.transnacala.lory.sync;

import android.content.Context;

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

        // Fetch active cadeiras in local DB to resolve valid IDs
        List<CadeiraEntity> cadeirasLocais = db.cadeiraDao().getCadeirasPendentes();
        String validCadeiraId = "cad-01";
        if (cadeirasLocais != null && !cadeirasLocais.isEmpty()) {
            validCadeiraId = cadeirasLocais.get(0).id;
        }

        // ====================================================================
        // PHASE 1: PUSH SYNC (CLIENT -> SERVIDOR)
        // ====================================================================

        // 1. Sync Eventos Pendentes
        List<EventoEntity> eventosPendentes = db.eventoDao().getEventosPendentes();
        if (!eventosPendentes.isEmpty()) {
            for (EventoEntity e : eventosPendentes) {
                if ("c1".equalsIgnoreCase(e.cadeiraId) || e.cadeiraId == null || e.cadeiraId.isEmpty()) {
                    e.cadeiraId = validCadeiraId;
                    db.eventoDao().update(e);
                }
            }

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

        // 2. Sync Cadeiras Pendentes
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

        // 3. Sync Enquetes Pendentes
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

        // 4. Sync Grupos Pendentes
        List<GrupoEntity> gruposPendentes = db.grupoDao().getGruposLiveData().getValue();
        if (gruposPendentes != null && !gruposPendentes.isEmpty()) {
            try {
                Response<Map<String, Object>> response = api.sincronizarGrupos(gruposPendentes).execute();
                if (response.isSuccessful()) {
                    for (GrupoEntity g : gruposPendentes) {
                        g.syncStatus = Constants.SYNC_STATUS_SYNCED;
                        db.grupoDao().update(g);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ====================================================================
        // PHASE 2: PULL SYNC (SERVIDOR -> CLIENTE / ROOM)
        // ====================================================================

        // Pull Cadeiras
        try {
            Response<List<CadeiraEntity>> resCadeiras = api.getCadeirasServidor().execute();
            if (resCadeiras.isSuccessful() && resCadeiras.body() != null) {
                for (CadeiraEntity c : resCadeiras.body()) {
                    c.syncStatus = Constants.SYNC_STATUS_SYNCED;
                    db.cadeiraDao().insert(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Pull Enquetes
        try {
            Response<List<EnqueteEntity>> resEnquetes = api.getEnquetesServidor().execute();
            if (resEnquetes.isSuccessful() && resEnquetes.body() != null) {
                for (EnqueteEntity eq : resEnquetes.body()) {
                    eq.syncStatus = Constants.SYNC_STATUS_SYNCED;
                    db.enqueteDao().insert(eq);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Pull Eventos
        try {
            Response<List<EventoEntity>> resEventos = api.getEventosServidor().execute();
            if (resEventos.isSuccessful() && resEventos.body() != null) {
                for (EventoEntity ev : resEventos.body()) {
                    ev.syncStatus = Constants.SYNC_STATUS_SYNCED;
                    db.eventoDao().insert(ev);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Pull Grupos
        try {
            Response<List<GrupoEntity>> resGrupos = api.getGruposServidor().execute();
            if (resGrupos.isSuccessful() && resGrupos.body() != null) {
                for (GrupoEntity g : resGrupos.body()) {
                    g.syncStatus = Constants.SYNC_STATUS_SYNCED;
                    db.grupoDao().insert(g);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allSuccess ? Result.success() : Result.retry();
    }
}