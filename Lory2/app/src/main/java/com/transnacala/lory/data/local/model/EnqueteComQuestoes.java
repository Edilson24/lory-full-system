package com.transnacala.lory.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.transnacala.lory.data.local.entity.EnqueteEntity;
import java.util.List;

public class EnqueteComQuestoes {
    @Embedded
    public EnqueteEntity enquete;

    @Relation(parentColumn = "id", entityColumn = "enqueteId", entity = com.transnacala.lory.data.local.entity.QuestaoEnqueteEntity.class)
    public List<QuestaoComOpcoes> questoes;
}
