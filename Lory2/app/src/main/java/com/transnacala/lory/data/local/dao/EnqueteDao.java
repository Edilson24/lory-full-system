package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.EnqueteEntity;

import java.util.List;

@Dao
public interface EnqueteDao {

    @Query("SELECT * FROM enquetes ORDER BY updatedAt DESC")
    LiveData<List<EnqueteEntity>> getEnquetesLiveData();

    @Query("SELECT * FROM enquetes WHERE syncStatus != 'SYNCED'")
    List<EnqueteEntity> getEnquetesPendentes();

    @Query("SELECT * FROM enquetes WHERE id = :id LIMIT 1")
    EnqueteEntity getById(String id);

    @Query("DELETE FROM enquetes WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Query("DELETE FROM enquetes WHERE syncStatus = 'SYNCED' AND id NOT IN (:serverIds)")
    void deleteSyncedExcept(List<String> serverIds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EnqueteEntity enquete);

    @Update
    void update(EnqueteEntity enquete);

    @Transaction
    default void reconcileWithServer(List<EnqueteEntity> serverEnquetes, List<String> acknowledgedIds) {
        if (serverEnquetes == null || serverEnquetes.isEmpty()) {
            return; // Do not delete local data if server returns empty
        }

        long now = System.currentTimeMillis();
        for (EnqueteEntity enqueteServidor : serverEnquetes) {
            EnqueteEntity local = getById(enqueteServidor.id);
            boolean isPending = local != null && !"SYNCED".equals(local.syncStatus);
            if (isPending && (acknowledgedIds == null || !acknowledgedIds.contains(enqueteServidor.id))) {
                continue;
            }
            enqueteServidor.syncStatus = "SYNCED";
            if (enqueteServidor.updatedAt == 0) {
                enqueteServidor.updatedAt = now;
            }
            insert(enqueteServidor);
        }
    }
}