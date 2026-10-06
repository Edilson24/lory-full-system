package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.GrupoEntity;

import java.util.List;

@Dao
public interface GrupoDao {

    @Query("SELECT * FROM grupos ORDER BY updatedAt DESC")
    LiveData<List<GrupoEntity>> getGruposLiveData();

    @Query("SELECT COUNT(*) FROM grupos")
    LiveData<Integer> getGruposCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GrupoEntity grupo);

    @Update
    void update(GrupoEntity grupo);
}