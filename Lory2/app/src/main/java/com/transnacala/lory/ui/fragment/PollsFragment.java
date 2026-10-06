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

import com.transnacala.lory.databinding.FragmentPollsBinding;
import com.transnacala.lory.ui.adapter.EnqueteAdapter;
import com.transnacala.lory.ui.viewmodel.PollsViewModel;

public class PollsFragment extends Fragment {

    private FragmentPollsBinding binding;
    private EnqueteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPollsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new EnqueteAdapter();
        binding.rvEnquetes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEnquetes.setAdapter(adapter);

        PollsViewModel viewModel = new ViewModelProvider(this).get(PollsViewModel.class);

        // Observe Enquetes from Room
        viewModel.getEnquetesLiveData().observe(getViewLifecycleOwner(), enquetes -> {
            if (enquetes != null && !enquetes.isEmpty()) {
                binding.rvEnquetes.setVisibility(View.VISIBLE);
                binding.tvEmptyStatePolls.setVisibility(View.GONE);
                adapter.setEnquetes(enquetes);
            } else {
                binding.rvEnquetes.setVisibility(View.GONE);
                binding.tvEmptyStatePolls.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}