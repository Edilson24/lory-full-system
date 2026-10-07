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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventoRepository {

    private final EventoDao eventoDao;
    private final Context context;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

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
        ioExecutor.execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            EventoEntity entity = new EventoEntity(id, cadeiraId, null, tipo, titulo, dataEvento, "PENDENTE", Constants.SYNC_STATUS_PENDING_INSERT, now);
            eventoDao.insert(entity);
            SyncManager.enqueueSync(context);
        });
    }

    public void updateEstado(String eventoId, String estado) {
        ioExecutor.execute(() -> {
            EventoEntity evento = eventoDao.getById(eventoId);
            if (evento == null) {
                return;
            }
            evento.estado = estado;
            evento.updatedAt = System.currentTimeMillis();
            if (!Constants.SYNC_STATUS_PENDING_INSERT.equals(evento.syncStatus)) {
                evento.syncStatus = Constants.SYNC_STATUS_PENDING_UPDATE;
            }
            eventoDao.update(evento);
            SyncManager.enqueueSync(context);
        });
    }

    public void deleteEvento(String eventoId) {
        ioExecutor.execute(() -> {
            EventoEntity evento = eventoDao.getById(eventoId);
            if (evento == null) {
                return;
            }
            if (Constants.SYNC_STATUS_PENDING_INSERT.equals(evento.syncStatus)) {
                eventoDao.deleteById(eventoId);
            } else {
                evento.syncStatus = Constants.SYNC_STATUS_PENDING_DELETE;
                evento.updatedAt = System.currentTimeMillis();
                eventoDao.update(evento);
            }
            SyncManager.enqueueSync(context);
        });
    }
}