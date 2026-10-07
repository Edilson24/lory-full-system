package com.transnacala.lory.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.repository.CadeiraRepository;
import com.transnacala.lory.sync.SyncManager;

import java.util.List;

public class CadeiraViewModel extends AndroidViewModel {

    private final CadeiraRepository repository;
    private final LiveData<List<CadeiraEntity>> cadeirasLiveData;

    public CadeiraViewModel(@NonNull Application application) {
        super(application);
        repository = new CadeiraRepository(application);
        cadeirasLiveData = repository.getCadeirasLiveData();
    }

    public LiveData<List<CadeiraEntity>> getCadeirasLiveData() {
        return cadeirasLiveData;
    }

    public void syncCadeiras() {
        SyncManager.enqueueSync(getApplication());
    }

    public void insertCadeira(String nome, String categoria, String turmaId) {
        repository.insertCadeira(nome, categoria, turmaId);
    }
}