package com.transnacala.lory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.EventoDao;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class EventoRepository {

    private final EventoDao eventoDao;
    private final Context context;

    public EventoRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.eventoDao = db.eventoDao();
    }

    public LiveData<List<EventoEntity>> getEventosLiveData() {
        return eventoDao.getEventosLiveData();
    }

    public LiveData<Integer> getEventosCountLiveData() {
        return eventoDao.getEventosCountLiveData();
    }

    public void insertEvento(String cadeiraId, String tipo, String titulo, String dataEvento) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            EventoEntity entity = new EventoEntity(id, cadeiraId, null, tipo, titulo, dataEvento, "PENDENTE", Constants.SYNC_STATUS_PENDING_INSERT, now);
            eventoDao.insert(entity);
            SyncManager.enqueueSync(context);
        });
    }
}