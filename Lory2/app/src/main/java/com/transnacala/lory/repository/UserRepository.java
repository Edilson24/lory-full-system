package com.transnacala.lory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.UtilizadorDao;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;

public class UserRepository {

    private final UtilizadorDao utilizadorDao;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        this.utilizadorDao = db.utilizadorDao();
    }

    public LiveData<UtilizadorEntity> getUtilizadorLiveData(String userId) {
        return utilizadorDao.getUtilizadorLiveData(userId);
    }

    public LiveData<Integer> getUtilizadoresCountLiveData() {
        return utilizadorDao.getUtilizadoresCountLiveData();
    }
}