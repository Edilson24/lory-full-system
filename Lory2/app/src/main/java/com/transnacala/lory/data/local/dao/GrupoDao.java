package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.local.model.GrupoComCadeira;

import java.util.List;

@Dao
public interface GrupoDao {

    @Query("SELECT grupos.*, cadeiras.nome AS cadeiraNome FROM grupos LEFT JOIN cadeiras ON grupos.cadeiraId = cadeiras.id ORDER BY grupos.updatedAt DESC")
    LiveData<List<GrupoComCadeira>> getGruposLiveData();

    @Query("SELECT COUNT(*) FROM grupos")
    LiveData<Integer> getGruposCountLiveData();

    @Query("SELECT * FROM grupos WHERE syncStatus != 'SYNCED'")
    List<GrupoEntity> getGruposPendentes();

    @Query("SELECT * FROM grupos WHERE id = :id LIMIT 1")
    GrupoEntity getById(String id);

    @Query("DELETE FROM grupos WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Query("DELETE FROM grupos WHERE syncStatus = 'SYNCED' AND id NOT IN (:serverIds)")
    void deleteSyncedExcept(List<String> serverIds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GrupoEntity grupo);

    @Update
    void update(GrupoEntity grupo);

    @Transaction
    default void reconcileWithServer(List<GrupoEntity> serverGrupos, List<String> acknowledgedIds) {
        if (serverGrupos == null || serverGrupos.isEmpty()) {
            return; // Do not delete local data if server returns empty
        }

        long now = System.currentTimeMillis();
        for (GrupoEntity grupoServidor : serverGrupos) {
            GrupoEntity local = getById(grupoServidor.id);
            boolean isPending = local != null && !"SYNCED".equals(local.syncStatus);
            if (isPending && (acknowledgedIds == null || !acknowledgedIds.contains(grupoServidor.id))) {
                continue;
            }
            grupoServidor.syncStatus = "SYNCED";
            if (grupoServidor.updatedAt == 0) {
                grupoServidor.updatedAt = now;
            }
            insert(grupoServidor);
        }
    }
}