package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.repository.CadeiraRepository;
import com.transnacala.lory.repository.EventoRepository;
import com.transnacala.lory.sync.SyncManager;

import java.util.List;

public class TasksViewModel extends AndroidViewModel {

    private final EventoRepository eventoRepository;
    private final CadeiraRepository cadeiraRepository;

    public TasksViewModel(@NonNull Application application) {
        super(application);
        eventoRepository = new EventoRepository(application);
        cadeiraRepository = new CadeiraRepository(application);
    }

    public LiveData<List<EventoEntity>> getEventosLiveData() {
        return eventoRepository.getEventosLiveData();
    }

    public LiveData<List<CadeiraEntity>> getCadeirasLiveData() {
        return cadeiraRepository.getCadeirasLiveData();
    }

    // Método para disparar a sincronização a partir da UI
    public void syncCadeiras() {
        // Dispara o agendamento no WorkManager
        //SyncManager.getInstance(getApplication()).enqueueOneTimeSync();

        // OU se o seu CadeiraRepository tiver um método próprio:
        // cadeiraRepository.syncCadeiras();
    }
}