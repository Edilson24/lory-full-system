package com.transnacala.lory.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.databinding.ItemEnqueteCardBinding;

import java.util.ArrayList;
import java.util.List;

public class EnqueteAdapter extends RecyclerView.Adapter<EnqueteAdapter.EnqueteViewHolder> {

    private final List<EnqueteEntity> enquetes = new ArrayList<>();

    public void setEnquetes(List<EnqueteEntity> newEnquetes) {
        this.enquetes.clear();
        if (newEnquetes != null) {
            this.enquetes.addAll(newEnquetes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EnqueteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEnqueteCardBinding binding = ItemEnqueteCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EnqueteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EnqueteViewHolder holder, int position) {
        holder.bind(enquetes.get(position));
    }

    @Override
    public int getItemCount() {
        return enquetes.size();
    }

    static class EnqueteViewHolder extends RecyclerView.ViewHolder {
        private final ItemEnqueteCardBinding binding;

        public EnqueteViewHolder(@NonNull ItemEnqueteCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(EnqueteEntity enquete) {
            binding.tvEnquetePergunta.setText(enquete.pergunta);
            String criador = enquete.criadaPor != null ? enquete.criadaPor : "Chefe de Turma";
            binding.tvEnqueteMeta.setText("Criado por: " + criador);
        }
    }
}