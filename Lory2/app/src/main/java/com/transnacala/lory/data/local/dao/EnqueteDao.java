package com.transnacala.lory.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.QuestaoEnqueteEntity;
import com.transnacala.lory.data.local.model.EnqueteComQuestoes;
import com.transnacala.lory.data.local.model.QuestaoComOpcoes;

import java.util.List;

@Dao
public interface EnqueteDao {

    @Transaction
    @Query("SELECT * FROM enquetes ORDER BY updatedAt DESC")
    LiveData<List<EnqueteComQuestoes>> getEnquetesLiveData();

    @Query("SELECT * FROM enquetes WHERE syncStatus != 'SYNCED'")
    List<EnqueteEntity> getEnquetesPendentes();

    @Query("SELECT * FROM enquetes WHERE id = :id LIMIT 1")
    EnqueteEntity getById(String id);

    @Query("DELETE FROM enquetes WHERE syncStatus = 'SYNCED'")
    void deleteSynced();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EnqueteEntity enquete);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestao(QuestaoEnqueteEntity questao);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestoes(List<QuestaoEnqueteEntity> questoes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOpcoes(List<OpcaoEnqueteEntity> opcoes);

    @Transaction
    @Query("SELECT * FROM questoes_enquete WHERE enqueteId = :enqueteId ORDER BY id")
    List<QuestaoComOpcoes> getQuestoesComOpcoes(String enqueteId);

    @Query("SELECT * FROM opcoes_enquete WHERE questaoId = :questaoId ORDER BY id")
    List<OpcaoEnqueteEntity> getOpcoes(String questaoId);

    @Query("SELECT * FROM opcoes_enquete WHERE id = :id LIMIT 1")
    OpcaoEnqueteEntity getOpcao(String id);

    @Query("SELECT * FROM questoes_enquete WHERE id = :id LIMIT 1")
    QuestaoEnqueteEntity getQuestao(String id);

    @Query("DELETE FROM opcoes_enquete WHERE questaoId IN " +
            "(SELECT id FROM questoes_enquete WHERE enqueteId = :enqueteId)")
    void deleteOpcoesDaEnquete(String enqueteId);

    @Query("DELETE FROM questoes_enquete WHERE enqueteId = :enqueteId")
    void deleteQuestoes(String enqueteId);

    @Query("UPDATE opcoes_enquete SET votosCount = MAX(0, votosCount + :delta) WHERE id = :opcaoId")
    void atualizarContagemVotos(String opcaoId, int delta);

    @Transaction
    default void insertWithQuestions(EnqueteEntity enquete, List<QuestaoEnqueteEntity> questoes,
                                     List<OpcaoEnqueteEntity> opcoes) {
        insert(enquete);
        insertQuestoes(questoes);
        if (!opcoes.isEmpty()) {
            insertOpcoes(opcoes);
        }
    }

    @Transaction
    default void reconcileWithServer(List<EnqueteEntity> serverEnquetes, List<String> acknowledgedIds) {
        long now = System.currentTimeMillis();
        for (EnqueteEntity poll : serverEnquetes) {
            EnqueteEntity local = getById(poll.id);
            boolean isPending = local != null && !"SYNCED".equals(local.syncStatus);
            if (isPending && (acknowledgedIds == null || !acknowledgedIds.contains(poll.id))) {
                continue;
            }
            poll.syncStatus = "SYNCED";
            if (poll.updatedAt == 0) {
                poll.updatedAt = now;
            }
            insert(poll);
            deleteOpcoesDaEnquete(poll.id);
            deleteQuestoes(poll.id);
            if (poll.questoes != null) {
                for (QuestaoEnqueteEntity question : poll.questoes) {
                    question.enqueteId = poll.id;
                    insertQuestao(question);
                    if (question.opcoes != null && !question.opcoes.isEmpty()) {
                        for (OpcaoEnqueteEntity option : question.opcoes) {
                            option.enqueteId = poll.id;
                            option.questaoId = question.id;
                        }
                        insertOpcoes(question.opcoes);
                    }
                }
            }
        }
    }
}
