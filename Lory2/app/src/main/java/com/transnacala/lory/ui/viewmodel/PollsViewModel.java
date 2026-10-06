package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.repository.EnqueteRepository;

import java.util.List;

public class PollsViewModel extends AndroidViewModel {

    private final EnqueteRepository enqueteRepository;

    public PollsViewModel(@NonNull Application application) {
        super(application);
        enqueteRepository = new EnqueteRepository(application);
    }

    public LiveData<List<EnqueteEntity>> getEnquetesLiveData() {
        return enqueteRepository.getEnquetesLiveData();
    }

    public void insertEnquete(String turmaId, String pergunta, String prazo, String criadaPor) {
        enqueteRepository.insertEnquete(turmaId, pergunta, prazo, criadaPor);
    }
}