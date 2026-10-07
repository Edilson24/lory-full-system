package com.transnacala.lory.data.remote.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GroupMembersRequest {
    @SerializedName("estudante_ids")
    public final List<String> estudanteIds;

    public GroupMembersRequest(List<String> estudanteIds) {
        this.estudanteIds = estudanteIds;
    }
}
