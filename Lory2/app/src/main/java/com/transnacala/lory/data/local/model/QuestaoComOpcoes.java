package com.transnacala.lory.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.QuestaoEnqueteEntity;

import java.util.List;

public class QuestaoComOpcoes {
    @Embedded
    public QuestaoEnqueteEntity questao;

    @Relation(parentColumn = "id", entityColumn = "questaoId")
    public List<OpcaoEnqueteEntity> opcoes;
}
