package com.transnacala.lory.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.dao.EnqueteDao;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.QuestaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.VotoLocalEntity;
import com.transnacala.lory.data.local.model.EnqueteComQuestoes;
import com.transnacala.lory.data.local.model.QuestaoEnqueteDraft;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.utils.Constants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnqueteRepository {
    private static final String TAG = "EnqueteRepository";

    private final EnqueteDao enqueteDao;
    private final AppDatabase database;
    private final Context context;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public EnqueteRepository(Context context) {
        this.context = context.getApplicationContext();
        database = AppDatabase.getInstance(this.context);
        this.enqueteDao = database.enqueteDao();
    }

    public LiveData<List<EnqueteComQuestoes>> getEnquetesLiveData() {
        return enqueteDao.getEnquetesLiveData();
    }

    public LiveData<List<VotoLocalEntity>> getVotosLiveData(String userId) {
        return database.votoLocalDao().getVotosLiveData(userId);
    }

    public void insertEnquete(String turmaId, String pergunta, String prazo, String criadaPor,
                              List<QuestaoEnqueteDraft> textosQuestoes) {
        ioExecutor.execute(() -> {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            EnqueteEntity entity = new EnqueteEntity(id, turmaId, pergunta, prazo, criadaPor, Constants.SYNC_STATUS_PENDING_INSERT, now);
            java.util.ArrayList<QuestaoEnqueteEntity> questoes = new java.util.ArrayList<>();
            java.util.ArrayList<OpcaoEnqueteEntity> opcoes = new java.util.ArrayList<>();
            for (QuestaoEnqueteDraft draft : textosQuestoes) {
                String questaoId = UUID.randomUUID().toString();
                questoes.add(new QuestaoEnqueteEntity(questaoId, id, draft.pergunta, now));
                for (String texto : draft.opcoes) {
                    opcoes.add(new OpcaoEnqueteEntity(
                            UUID.randomUUID().toString(), id, questaoId, texto, 0));
                }
            }
            enqueteDao.insertWithQuestions(entity, questoes, opcoes);
            SyncManager.enqueueSync(context);
        });
    }

    public void votar(String questaoId, String opcaoId, String userId) {
        ioExecutor.execute(() -> {
            boolean changed = database.runInTransaction(() -> saveVoteLocally(questaoId, opcaoId, userId));
            if (changed) {
                SyncManager.enqueueSync(context);
            }
        });
    }

    private boolean saveVoteLocally(String questaoId, String opcaoId, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.w(TAG, "Voto ignorado: utilizador sem sessão.");
            return false;
        }
        VotoLocalEntity votoAnterior = database.votoLocalDao().getVoto(questaoId, userId);
        if (votoAnterior != null && votoAnterior.opcaoId.equals(opcaoId)) {
            return false;
        }
        OpcaoEnqueteEntity novaOpcao = enqueteDao.getOpcao(opcaoId);
        if (novaOpcao == null || !questaoId.equals(novaOpcao.questaoId)) {
            Log.w(TAG, "Voto ignorado: opção não pertence à enquete.");
            return false;
        }
        if (votoAnterior != null) {
            enqueteDao.atualizarContagemVotos(votoAnterior.opcaoId, -1);
        }
        enqueteDao.atualizarContagemVotos(opcaoId, 1);
        database.votoLocalDao().save(new VotoLocalEntity(
                questaoId, userId, opcaoId, "PENDING", System.currentTimeMillis()));
        return true;
    }
}