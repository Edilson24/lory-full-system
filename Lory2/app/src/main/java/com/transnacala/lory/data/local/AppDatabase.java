package com.transnacala.lory.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.transnacala.lory.data.local.dao.CadeiraDao;
import com.transnacala.lory.data.local.dao.EnqueteDao;
import com.transnacala.lory.data.local.dao.EventoDao;
import com.transnacala.lory.data.local.dao.GrupoDao;
import com.transnacala.lory.data.local.dao.UtilizadorDao;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;

@Database(entities = {
        EventoEntity.class,
        CadeiraEntity.class,
        EnqueteEntity.class,
        OpcaoEnqueteEntity.class,
        GrupoEntity.class,
        UtilizadorEntity.class
}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract EventoDao eventoDao();
    public abstract CadeiraDao cadeiraDao();
    public abstract EnqueteDao enqueteDao();
    public abstract GrupoDao grupoDao();
    public abstract UtilizadorDao utilizadorDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "lory_local_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}