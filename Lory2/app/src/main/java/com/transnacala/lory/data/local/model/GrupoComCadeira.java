package com.transnacala.lory.data.local.model;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.transnacala.lory.data.local.entity.GrupoEntity;

public class GrupoComCadeira {

    @Embedded
    public GrupoEntity grupo;

    @ColumnInfo(name = "cadeiraNome")
    public String cadeiraNome;
}
