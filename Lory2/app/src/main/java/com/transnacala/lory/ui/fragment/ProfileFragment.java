package com.transnacala.lory.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.transnacala.lory.R;
import com.transnacala.lory.databinding.FragmentProfileBinding;
import com.transnacala.lory.ui.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Load session user details
        String name = viewModel.getSessionManager().getUserName();
        String role = viewModel.getSessionManager().isChefe() ? getString(R.string.role_chefe) : getString(R.string.role_estudante);

        binding.tvProfileName.setText(name);
        binding.tvProfileRole.setText(role);

        // Observe Room counters dynamically
        viewModel.getCadeirasCountLiveData().observe(getViewLifecycleOwner(), count ->
            binding.tvStatCadeirasCount.setText(String.valueOf(count != null ? count : 0))
        );

        viewModel.getEventosCountLiveData().observe(getViewLifecycleOwner(), count ->
            binding.tvStatEventosCount.setText(String.valueOf(count != null ? count : 0))
        );

        viewModel.getEstudantesCountLiveData().observe(getViewLifecycleOwner(), count ->
            binding.tvStatEstudantesCount.setText(String.valueOf(count != null ? count : 0))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}