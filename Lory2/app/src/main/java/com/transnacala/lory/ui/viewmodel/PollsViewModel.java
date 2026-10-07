package com.transnacala.lory.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.entity.VotoLocalEntity;
import com.transnacala.lory.data.local.model.EnqueteComQuestoes;
import com.transnacala.lory.repository.EnqueteRepository;

import java.util.List;

public class PollsViewModel extends AndroidViewModel {

    private final EnqueteRepository enqueteRepository;

    public PollsViewModel(@NonNull Application application) {
        super(application);
        enqueteRepository = new EnqueteRepository(application);
    }

    public LiveData<List<EnqueteComQuestoes>> getEnquetesLiveData() {
        return enqueteRepository.getEnquetesLiveData();
    }

    public LiveData<List<VotoLocalEntity>> getVotosLiveData(String userId) {
        return enqueteRepository.getVotosLiveData(userId);
    }

    public void votar(String questaoId, String opcaoId, String userId) {
        enqueteRepository.votar(questaoId, opcaoId, userId);
    }
}