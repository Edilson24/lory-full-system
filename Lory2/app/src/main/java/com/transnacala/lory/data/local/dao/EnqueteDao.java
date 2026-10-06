package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.EnqueteEntity;

import java.util.List;

@Dao
public interface EnqueteDao {

    @Query("SELECT * FROM enquetes ORDER BY updatedAt DESC")
    LiveData<List<EnqueteEntity>> getEnquetesLiveData();

    @Query("SELECT * FROM enquetes WHERE syncStatus != 'SYNCED'")
    List<EnqueteEntity> getEnquetesPendentes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EnqueteEntity enquete);

    @Update
    void update(EnqueteEntity enquete);
}