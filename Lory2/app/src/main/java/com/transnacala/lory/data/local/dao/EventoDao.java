package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.EventoEntity;

import java.util.List;

@Dao
public interface EventoDao {

    @Query("SELECT * FROM eventos ORDER BY updatedAt DESC")
    LiveData<List<EventoEntity>> getEventosLiveData();

    @Query("SELECT * FROM eventos WHERE syncStatus != 'SYNCED'")
    List<EventoEntity> getEventosPendentes();

    @Query("SELECT * FROM eventos WHERE id = :id LIMIT 1")
    EventoEntity getById(String id);

    @Query("DELETE FROM eventos WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Query("DELETE FROM eventos WHERE syncStatus = 'SYNCED' AND id NOT IN (:serverIds)")
    void deleteSyncedExcept(List<String> serverIds);

    @Query("SELECT COUNT(*) FROM eventos")
    LiveData<Integer> getEventosCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventoEntity evento);

    @Update
    void update(EventoEntity evento);

    @Transaction
    default void reconcileWithServer(List<EventoEntity> serverEventos, List<String> acknowledgedIds) {
        if (serverEventos == null || serverEventos.isEmpty()) {
            return; // Do not delete local data if server returns empty
        }

        long now = System.currentTimeMillis();
        for (EventoEntity eventoServidor : serverEventos) {
            EventoEntity local = getById(eventoServidor.id);
            boolean isPending = local != null && !"SYNCED".equals(local.syncStatus);
            if (isPending && (acknowledgedIds == null || !acknowledgedIds.contains(eventoServidor.id))) {
                continue;
            }
            eventoServidor.syncStatus = "SYNCED";
            if (eventoServidor.updatedAt == 0) {
                eventoServidor.updatedAt = now;
            }
            insert(eventoServidor);
        }
    }
}