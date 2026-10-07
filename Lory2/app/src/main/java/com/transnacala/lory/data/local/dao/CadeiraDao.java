package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.CadeiraEntity;

import java.util.List;

@Dao
public interface CadeiraDao {

    @Query("SELECT * FROM cadeiras ORDER BY nome ASC")
    LiveData<List<CadeiraEntity>> getCadeirasLiveData();

    @Query("SELECT * FROM cadeiras WHERE syncStatus != 'SYNCED'")
    List<CadeiraEntity> getCadeirasPendentes();

    @Query("SELECT * FROM cadeiras ORDER BY nome ASC")
    List<CadeiraEntity> getAllCadeiras();

    @Query("SELECT * FROM cadeiras WHERE id = :id LIMIT 1")
    CadeiraEntity getById(String id);

    @Query("DELETE FROM cadeiras WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Query("DELETE FROM cadeiras WHERE syncStatus = 'SYNCED' AND id NOT IN (:serverIds)")
    void deleteSyncedExcept(List<String> serverIds);

    @Query("SELECT COUNT(*) FROM cadeiras")
    LiveData<Integer> getCadeirasCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CadeiraEntity> cadeiras);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CadeiraEntity cadeira);

    @Update
    void update(CadeiraEntity cadeira);

    @Transaction
    default void reconcileWithServer(List<CadeiraEntity> serverCadeiras, List<String> acknowledgedIds) {
        long now = System.currentTimeMillis();
        List<String> serverIds = new java.util.ArrayList<>();
        for (CadeiraEntity cadeiraServidor : serverCadeiras) {
            serverIds.add(cadeiraServidor.id);
            CadeiraEntity local = getById(cadeiraServidor.id);
            boolean isPending = local != null && !"SYNCED".equals(local.syncStatus);
            if (isPending && (acknowledgedIds == null || !acknowledgedIds.contains(cadeiraServidor.id))) {
                continue;
            }
            cadeiraServidor.syncStatus = "SYNCED";
            if (cadeiraServidor.updatedAt == 0) {
                cadeiraServidor.updatedAt = now;
            }
            insert(cadeiraServidor);
        }
        if (serverIds.isEmpty()) {
            deleteSynced();
        } else {
            deleteSyncedExcept(serverIds);
        }
    }
}