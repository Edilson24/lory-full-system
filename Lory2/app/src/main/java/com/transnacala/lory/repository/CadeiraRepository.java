package com.transnacala.lory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.CadeiraDao;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class CadeiraRepository {

    private final CadeiraDao cadeiraDao;
    private final Context context;

    public CadeiraRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.cadeiraDao = db.cadeiraDao();
    }

    public LiveData<List<CadeiraEntity>> getCadeirasLiveData() {
        return cadeiraDao.getCadeirasLiveData();
    }

    public LiveData<Integer> getCadeirasCountLiveData() {
        return cadeiraDao.getCadeirasCountLiveData();
    }

    public void insertCadeira(String nome, String categoria, String turmaId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            CadeiraEntity entity = new CadeiraEntity(id, turmaId, "sem1", "doc1", nome, categoria, 0, false, Constants.SYNC_STATUS_PENDING_INSERT, now);
            cadeiraDao.insert(entity);
            SyncManager.enqueueSync(context);
        });
    }
}