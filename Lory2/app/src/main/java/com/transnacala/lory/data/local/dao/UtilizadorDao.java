package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.transnacala.lory.data.local.entity.UtilizadorEntity;

import java.util.List;

@Dao
public interface UtilizadorDao {

    @Query("SELECT * FROM utilizadores WHERE id = :userId LIMIT 1")
    LiveData<UtilizadorEntity> getUtilizadorLiveData(String userId);

    @Query("SELECT COUNT(*) FROM utilizadores")
    LiveData<Integer> getUtilizadoresCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UtilizadorEntity utilizador);
}