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

    @Query("SELECT * FROM eventos WHERE syncStatus != 'PENDING_DELETE' " +
            "ORDER BY CASE WHEN estado = 'CONCLUIDO' THEN 1 ELSE 0 END, " +
            "datetime(replace(dataEvento, 'T', ' ')) ASC")
    LiveData<List<EventoEntity>> getEventosLiveData();

    @Query("SELECT * FROM eventos WHERE syncStatus != 'SYNCED' AND syncStatus != 'PENDING_DELETE'")
    List<EventoEntity> getEventosPendentes();

    @Query("SELECT * FROM eventos WHERE syncStatus = 'PENDING_DELETE'")
    List<EventoEntity> getEventosPendentesDelete();

    @Query("SELECT * FROM eventos WHERE id = :id LIMIT 1")
    EventoEntity getById(String id);

    @Query("DELETE FROM eventos WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Query("DELETE FROM eventos WHERE syncStatus = 'SYNCED' AND id NOT IN (:serverIds)")
    void deleteSyncedExcept(List<String> serverIds);

    @Query("DELETE FROM eventos WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT COUNT(*) FROM eventos WHERE syncStatus != 'PENDING_DELETE'")
    LiveData<Integer> getEventosCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventoEntity evento);

    @Update
    void update(EventoEntity evento);

    @Transaction
    default void reconcileWithServer(List<EventoEntity> serverEventos, List<String> acknowledgedIds) {
        long now = System.currentTimeMillis();
        List<String> serverIds = new java.util.ArrayList<>();
        for (EventoEntity eventoServidor : serverEventos) {
            serverIds.add(eventoServidor.id);
            EventoEntity local = getById(eventoServidor.id);
            if (local != null && "PENDING_DELETE".equals(local.syncStatus)) {
                continue;
            }
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
        if (serverIds.isEmpty()) {
            deleteSynced();
        } else {
            deleteSyncedExcept(serverIds);
        }
    }
}