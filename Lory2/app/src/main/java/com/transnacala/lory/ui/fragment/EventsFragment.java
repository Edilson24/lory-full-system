package com.transnacala.lory.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.appcompat.app.AlertDialog;

import com.transnacala.lory.databinding.FragmentEventsBinding;
import com.transnacala.lory.ui.adapter.EventoAdapter;
import com.transnacala.lory.ui.viewmodel.EventsViewModel;
import com.transnacala.lory.utils.SessionManager;

public class EventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventsViewModel viewModel = new ViewModelProvider(this).get(EventsViewModel.class);
        adapter = new EventoAdapter(new EventoAdapter.OnEventoActionListener() {
            @Override
            public void onEstadoChange(com.transnacala.lory.data.local.entity.EventoEntity evento) {
                String novoEstado = "CONCLUIDO".equalsIgnoreCase(evento.estado) ? "PENDENTE" : "CONCLUIDO";
                viewModel.updateEstado(evento.id, novoEstado);
            }

            @Override
            public void onDelete(com.transnacala.lory.data.local.entity.EventoEntity evento) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar evento")
                        .setMessage("Deseja eliminar \"" + evento.titulo + "\"?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (dialog, which) -> viewModel.deleteEvento(evento.id))
                        .show();
            }
        }, new SessionManager(requireContext()).isChefe());
        binding.rvEventos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEventos.setAdapter(adapter);

        viewModel.getEventosLiveData().observe(getViewLifecycleOwner(), eventos -> {
            boolean hasEvents = eventos != null && !eventos.isEmpty();
            binding.rvEventos.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
            binding.tvEmptyStateEventos.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
            adapter.setEventos(eventos);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}
