package com.transnacala.lory.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.transnacala.lory.data.local.entity.EventoEntity;

import java.util.List;

@Dao
public interface EventoDao {

    @Query("SELECT * FROM eventos WHERE syncStatus != 'SYNCED'")
    List<EventoEntity> getEventosPendentes();

    @Query("SELECT * FROM eventos")
    List<EventoEntity> getAllEventos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventoEntity evento);

    @Update
    void update(EventoEntity evento);
}