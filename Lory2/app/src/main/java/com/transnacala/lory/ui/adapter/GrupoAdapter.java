package com.transnacala.lory.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.lory.R;
import com.transnacala.lory.data.local.model.GrupoComCadeira;
import com.transnacala.lory.databinding.ItemGrupoCardBinding;
import com.transnacala.lory.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class GrupoAdapter extends RecyclerView.Adapter<GrupoAdapter.GrupoViewHolder> {

    public interface OnGrupoClickListener {
        void onGrupoClick(GrupoComCadeira grupo);
    }

    private final List<GrupoComCadeira> grupos = new ArrayList<>();
    private final OnGrupoClickListener listener;

    public GrupoAdapter(OnGrupoClickListener listener) {
        this.listener = listener;
    }

    public void setGrupos(List<GrupoComCadeira> novosGrupos) {
        grupos.clear();
        if (novosGrupos != null) {
            grupos.addAll(novosGrupos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GrupoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGrupoCardBinding binding = ItemGrupoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GrupoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GrupoViewHolder holder, int position) {
        GrupoComCadeira grupo = grupos.get(position);
        holder.bind(grupo);
        holder.itemView.setOnClickListener(v -> listener.onGrupoClick(grupo));
    }

    @Override
    public int getItemCount() {
        return grupos.size();
    }

    static class GrupoViewHolder extends RecyclerView.ViewHolder {
        private final ItemGrupoCardBinding binding;

        GrupoViewHolder(@NonNull ItemGrupoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GrupoComCadeira grupoComCadeira) {
            String cadeiraNome = grupoComCadeira.cadeiraNome == null
                    ? grupoComCadeira.grupo.cadeiraId : grupoComCadeira.cadeiraNome;
            binding.tvGrupoNome.setText(grupoComCadeira.grupo.nome);
            binding.tvGrupoCadeira.setText(itemView.getContext().getString(R.string.group_chair_format, cadeiraNome));
            binding.tvGrupoTema.setText(grupoComCadeira.grupo.tema == null || grupoComCadeira.grupo.tema.isEmpty()
                    ? itemView.getContext().getString(R.string.group_no_theme) : grupoComCadeira.grupo.tema);
            boolean pending = Constants.SYNC_STATUS_PENDING_INSERT.equals(grupoComCadeira.grupo.syncStatus)
                    || Constants.SYNC_STATUS_PENDING_UPDATE.equals(grupoComCadeira.grupo.syncStatus);
            binding.tvGrupoSync.setText(pending ? R.string.event_sync_pending : R.string.event_sync_complete);
        }
    }
}
