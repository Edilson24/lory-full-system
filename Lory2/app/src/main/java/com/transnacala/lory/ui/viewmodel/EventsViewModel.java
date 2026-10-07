package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.repository.EventoRepository;

import java.util.List;

public class EventsViewModel extends AndroidViewModel {

    private final EventoRepository eventoRepository;

    public EventsViewModel(@NonNull Application application) {
        super(application);
        eventoRepository = new EventoRepository(application);
    }

    public LiveData<List<EventoEntity>> getEventosLiveData() {
        return eventoRepository.getEventosLiveData();
    }

    public void updateEstado(String eventoId, String estado) {
        eventoRepository.updateEstado(eventoId, estado);
    }

    public void deleteEvento(String eventoId) {
        eventoRepository.deleteEvento(eventoId);
    }
}
