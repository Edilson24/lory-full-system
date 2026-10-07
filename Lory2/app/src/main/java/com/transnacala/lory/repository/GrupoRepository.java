package com.transnacala.lory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.GrupoDao;
import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.local.model.GrupoComCadeira;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class GrupoRepository {

    private final GrupoDao grupoDao;
    private final Context context;

    public GrupoRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.grupoDao = db.grupoDao();
    }

    public LiveData<List<GrupoComCadeira>> getGruposLiveData() {
        return grupoDao.getGruposLiveData();
    }

    public LiveData<Integer> getGruposCountLiveData() {
        return grupoDao.getGruposCountLiveData();
    }

    public void insertGrupo(String cadeiraId, String nome, String tema) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            GrupoEntity entity = new GrupoEntity(id, cadeiraId, nome, tema, "Outubro 2026", "Em Andamento", Constants.SYNC_STATUS_PENDING_INSERT, now);
            grupoDao.insert(entity);
            SyncManager.enqueueSync(context);
        });
    }
}