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

    public interface OnCadeiraClickListener {
        void onCadeiraClick(CadeiraEntity cadeira);
    }

    private final List<CadeiraEntity> cadeiras = new ArrayList<>();
    private OnCadeiraClickListener listener;

    public void setOnCadeiraClickListener(OnCadeiraClickListener listener) {
        this.listener = listener;
    }

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
        CadeiraEntity cadeira = cadeiras.get(position);
        holder.bind(cadeira);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCadeiraClick(cadeira);
            }
        });
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
            binding.tvCadeiraDocente.setText(cadeira.docenteNome != null && !cadeira.docenteNome.isEmpty()
                    ? cadeira.docenteNome : "Docente não informado");
            binding.tvCadeiraStatus.setText(cadeira.concluida ?
                    itemView.getContext().getString(R.string.project_completed) :
                    itemView.getContext().getString(R.string.project_in_progress));
            binding.pbCadeiraProgress.setProgress(cadeira.progresso > 0 ? cadeira.progresso : 50);
        }
    }
}