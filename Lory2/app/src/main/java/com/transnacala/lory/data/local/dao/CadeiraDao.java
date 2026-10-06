package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.CadeiraEntity;

import java.util.List;

@Dao
public interface CadeiraDao {

    @Query("SELECT * FROM cadeiras ORDER BY updatedAt DESC")
    LiveData<List<CadeiraEntity>> getCadeirasLiveData();

    @Query("SELECT * FROM cadeiras WHERE syncStatus != 'SYNCED'")
    List<CadeiraEntity> getCadeirasPendentes();

    @Query("SELECT COUNT(*) FROM cadeiras")
    LiveData<Integer> getCadeirasCountLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CadeiraEntity> cadeiras);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CadeiraEntity cadeira);

    @Update
    void update(CadeiraEntity cadeira);
}