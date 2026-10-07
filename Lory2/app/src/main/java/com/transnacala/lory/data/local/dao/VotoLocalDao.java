package com.transnacala.lory.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.lifecycle.LiveData;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.transnacala.lory.data.local.entity.VotoLocalEntity;

import java.util.List;

@Dao
public interface VotoLocalDao {
    @Query("SELECT * FROM votos_locais WHERE estudanteId = :estudanteId")
    LiveData<List<VotoLocalEntity>> getVotosLiveData(String estudanteId);

    @Query("SELECT * FROM votos_locais WHERE syncStatus != 'SYNCED'")
    List<VotoLocalEntity> getPendentes();

    @Query("SELECT * FROM votos_locais WHERE questaoId = :questaoId AND estudanteId = :estudanteId LIMIT 1")
    VotoLocalEntity getVoto(String questaoId, String estudanteId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(VotoLocalEntity voto);

    @Query("UPDATE votos_locais SET syncStatus = 'SYNCED' WHERE questaoId = :questaoId AND estudanteId = :estudanteId")
    void marcarSincronizado(String questaoId, String estudanteId);
}
