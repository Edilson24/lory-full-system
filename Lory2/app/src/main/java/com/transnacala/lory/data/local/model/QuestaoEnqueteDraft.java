package com.transnacala.lory.data.local.model;

import java.util.List;

public class QuestaoEnqueteDraft {
    public final String pergunta;
    public final List<String> opcoes;

    public QuestaoEnqueteDraft(String pergunta, List<String> opcoes) {
        this.pergunta = pergunta;
        this.opcoes = opcoes;
    }
}
