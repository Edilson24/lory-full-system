package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;
import com.transnacala.lory.repository.CadeiraRepository;
import com.transnacala.lory.repository.EventoRepository;
import com.transnacala.lory.repository.GrupoRepository;
import com.transnacala.lory.repository.UserRepository;
import com.transnacala.lory.utils.SessionManager;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final CadeiraRepository cadeiraRepository;
    private final EventoRepository eventoRepository;
    private final GrupoRepository grupoRepository;
    private final SessionManager sessionManager;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        cadeiraRepository = new CadeiraRepository(application);
        eventoRepository = new EventoRepository(application);
        grupoRepository = new GrupoRepository(application);
        sessionManager = new SessionManager(application);
    }

    public LiveData<UtilizadorEntity> getUtilizadorLiveData() {
        return userRepository.getUtilizadorLiveData(sessionManager.getUserId());
    }

    public LiveData<Integer> getCadeirasCountLiveData() {
        return cadeiraRepository.getCadeirasCountLiveData();
    }

    public LiveData<Integer> getEventosCountLiveData() {
        return eventoRepository.getEventosCountLiveData();
    }

    public LiveData<Integer> getEstudantesCountLiveData() {
        return userRepository.getUtilizadoresCountLiveData();
    }

    public LiveData<List<GrupoEntity>> getGruposLiveData() {
        return grupoRepository.getGruposLiveData();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}