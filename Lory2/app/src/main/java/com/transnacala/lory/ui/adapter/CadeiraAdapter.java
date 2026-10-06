package com.transnacala.lory.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.lory.R;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.databinding.ItemCadeiraCardBinding;

import java.util.ArrayList;
import java.util.List;

public class CadeiraAdapter extends RecyclerView.Adapter<CadeiraAdapter.CadeiraViewHolder> {

    private final List<CadeiraEntity> cadeiras = new ArrayList<>();

    public void setCadeiras(List<CadeiraEntity> newCadeiras) {
        this.cadeiras.clear();
        if (newCadeiras != null) {
            this.cadeiras.addAll(newCadeiras);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CadeiraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCadeiraCardBinding binding = ItemCadeiraCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CadeiraViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CadeiraViewHolder holder, int position) {
        holder.bind(cadeiras.get(position));
    }

    @Override
    public int getItemCount() {
        return cadeiras.size();
    }

    static class CadeiraViewHolder extends RecyclerView.ViewHolder {
        private final ItemCadeiraCardBinding binding;

        public CadeiraViewHolder(@NonNull ItemCadeiraCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CadeiraEntity cadeira) {
            binding.tvCadeiraNome.setText(cadeira.nome);
            binding.tvCadeiraCategoria.setText(cadeira.categoria != null && !cadeira.categoria.isEmpty() ? cadeira.categoria : "1º Semestre 2026");
            binding.tvCadeiraStatus.setText(cadeira.concluida ?
                    itemView.getContext().getString(R.string.project_completed) :
                    itemView.getContext().getString(R.string.project_in_progress));
            binding.pbCadeiraProgress.setProgress(cadeira.progresso > 0 ? cadeira.progresso : 50);
        }
    }
}