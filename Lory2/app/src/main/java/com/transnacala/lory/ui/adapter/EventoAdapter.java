package com.transnacala.lory.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.lory.R;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.databinding.ItemEventoCardBinding;
import com.transnacala.lory.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    private final List<EventoEntity> eventos = new ArrayList<>();

    public void setEventos(List<EventoEntity> novosEventos) {
        eventos.clear();
        if (novosEventos != null) {
            eventos.addAll(novosEventos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventoCardBinding binding = ItemEventoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        holder.bind(eventos.get(position));
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        private final ItemEventoCardBinding binding;

        EventoViewHolder(@NonNull ItemEventoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EventoEntity evento) {
            binding.tvEventoTitulo.setText(evento.titulo);
            binding.tvEventoTipo.setText(getTypeLabel(evento.tipo));
            binding.tvEventoData.setText(evento.dataEvento == null ? "" : evento.dataEvento.replace('T', ' '));
            binding.tvEventoEstado.setText("CONCLUIDO".equalsIgnoreCase(evento.estado)
                    ? R.string.event_state_completed : R.string.event_state_pending);
            binding.tvEventoSync.setText(isPending(evento.syncStatus)
                    ? R.string.event_sync_pending : R.string.event_sync_complete);
        }

        private int getTypeLabel(String type) {
            if ("TESTE".equalsIgnoreCase(type)) {
                return R.string.event_type_test;
            }
            if ("APRESENTACAO".equalsIgnoreCase(type)) {
                return R.string.event_type_presentation;
            }
            return R.string.event_type_other;
        }

        private boolean isPending(String syncStatus) {
            return Constants.SYNC_STATUS_PENDING_INSERT.equals(syncStatus)
                    || Constants.SYNC_STATUS_PENDING_UPDATE.equals(syncStatus);
        }
    }
}
