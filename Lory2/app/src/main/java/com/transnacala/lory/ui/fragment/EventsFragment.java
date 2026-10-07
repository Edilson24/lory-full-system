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

import com.transnacala.lory.databinding.FragmentEventsBinding;
import com.transnacala.lory.ui.adapter.EventoAdapter;
import com.transnacala.lory.ui.viewmodel.EventsViewModel;

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

        adapter = new EventoAdapter();
        binding.rvEventos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEventos.setAdapter(adapter);

        EventsViewModel viewModel = new ViewModelProvider(this).get(EventsViewModel.class);
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
