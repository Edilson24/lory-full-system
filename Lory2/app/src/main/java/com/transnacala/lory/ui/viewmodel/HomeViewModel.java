package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.repository.CadeiraRepository;
import com.transnacala.lory.repository.EventoRepository;
import com.transnacala.lory.utils.SessionManager;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final CadeiraRepository cadeiraRepository;
    private final EventoRepository eventoRepository;
    private final SessionManager sessionManager;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        cadeiraRepository = new CadeiraRepository(application);
        eventoRepository = new EventoRepository(application);
        sessionManager = new SessionManager(application);
    }

    public LiveData<List<CadeiraEntity>> getCadeirasLiveData() {
        return cadeiraRepository.getCadeirasLiveData();
    }

    public LiveData<List<EventoEntity>> getEventosLiveData() {
        return eventoRepository.getEventosLiveData();
    }

    public String getUserName() {
        return sessionManager.getUserName();
    }

    public String getUserRole() {
        return sessionManager.getUserRole();
    }

    public boolean isChefe() {
        return sessionManager.isChefe();
    }
}