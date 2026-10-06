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

import com.transnacala.lory.databinding.FragmentTasksBinding;
import com.transnacala.lory.ui.adapter.CadeiraAdapter;
import com.transnacala.lory.ui.viewmodel.TasksViewModel;

public class TasksFragment extends Fragment {

    private FragmentTasksBinding binding;
    private CadeiraAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new CadeiraAdapter();
        binding.rvCadeiras.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCadeiras.setAdapter(adapter);

        TasksViewModel viewModel = new ViewModelProvider(this).get(TasksViewModel.class);

        // Observe Cadeiras from Room Database
        viewModel.getCadeirasLiveData().observe(getViewLifecycleOwner(), cadeiras -> {
            if (cadeiras != null && !cadeiras.isEmpty()) {
                binding.rvCadeiras.setVisibility(View.VISIBLE);
                binding.tvEmptyStateTasks.setVisibility(View.GONE);
                adapter.setCadeiras(cadeiras);
            } else {
                binding.rvCadeiras.setVisibility(View.GONE);
                binding.tvEmptyStateTasks.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}