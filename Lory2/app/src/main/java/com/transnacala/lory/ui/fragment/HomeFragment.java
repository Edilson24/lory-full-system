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
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.databinding.FragmentHomeBinding;
import com.transnacala.lory.ui.viewmodel.HomeViewModel;

import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Real user name from active session
        String userName = viewModel.getUserName();
        binding.tvGreetingName.setText(getString(R.string.greeting_format, userName));

        // Time-of-day greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            binding.tvGreetingSubtitle.setText(getString(R.string.good_morning));
        } else if (hour >= 12 && hour < 18) {
            binding.tvGreetingSubtitle.setText(getString(R.string.good_afternoon));
        } else {
            binding.tvGreetingSubtitle.setText(getString(R.string.good_evening));
        }

        // Observe Room Database for real Cadeiras
        viewModel.getCadeirasLiveData().observe(getViewLifecycleOwner(), cadeiras -> {
            if (cadeiras != null && !cadeiras.isEmpty()) {
                binding.gridProjects.setVisibility(View.VISIBLE);
                binding.tvEmptyStateProjects.setVisibility(View.GONE);
                updateCadeirasUI(cadeiras);
            } else {
                binding.gridProjects.setVisibility(View.GONE);
                binding.tvEmptyStateProjects.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateCadeirasUI(List<CadeiraEntity> cadeiras) {
        if (!cadeiras.isEmpty()) {
            CadeiraEntity c1 = cadeiras.get(0);
            binding.card1Title.setText(c1.nome);
            binding.card1Sub.setText(c1.categoria != null ? c1.categoria : "Cadeira");
            binding.card1Pct.setText(getString(R.string.pct_format, c1.progresso));
            binding.card1Progress.setProgress(c1.progresso);
            binding.card1Container.setVisibility(View.VISIBLE);
        } else {
            binding.card1Container.setVisibility(View.GONE);
        }

        if (cadeiras.size() > 1) {
            CadeiraEntity c2 = cadeiras.get(1);
            binding.card2Title.setText(c2.nome);
            binding.card2Sub.setText(c2.categoria != null ? c2.categoria : "Cadeira");
            binding.card2Pct.setText(getString(R.string.pct_format, c2.progresso));
            binding.card2Progress.setProgress(c2.progresso);
            binding.card2Container.setVisibility(View.VISIBLE);
        } else {
            binding.card2Container.setVisibility(View.GONE);
        }

        if (cadeiras.size() > 2) {
            CadeiraEntity c3 = cadeiras.get(2);
            binding.card3Title.setText(c3.nome);
            binding.card3Sub.setText(c3.categoria != null ? c3.categoria : "Cadeira");
            binding.card3Pct.setText(getString(R.string.pct_format, c3.progresso));
            binding.card3Progress.setProgress(c3.progresso);
            binding.card3Container.setVisibility(View.VISIBLE);
        } else {
            binding.card3Container.setVisibility(View.GONE);
        }

        if (cadeiras.size() > 3) {
            CadeiraEntity c4 = cadeiras.get(3);
            binding.card4Title.setText(c4.nome);
            binding.card4Sub.setText(c4.categoria != null ? c4.categoria : "Cadeira");
            binding.card4Pct.setText(getString(R.string.pct_format, c4.progresso));
            binding.card4Progress.setProgress(c4.progresso);
            binding.card4Container.setVisibility(View.VISIBLE);
        } else {
            binding.card4Container.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}