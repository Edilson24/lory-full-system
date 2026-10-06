package com.transnacala.lory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.EnqueteDao;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class EnqueteRepository {

    private final EnqueteDao enqueteDao;
    private final Context context;

    public EnqueteRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.enqueteDao = db.enqueteDao();
    }

    public LiveData<List<EnqueteEntity>> getEnquetesLiveData() {
        return enqueteDao.getEnquetesLiveData();
    }

    public void insertEnquete(String turmaId, String pergunta, String prazo, String criadaPor) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            EnqueteEntity entity = new EnqueteEntity(id, turmaId, pergunta, prazo, criadaPor, Constants.SYNC_STATUS_PENDING_INSERT, now);
            enqueteDao.insert(entity);
            SyncManager.enqueueSync(context);
        });
    }
}