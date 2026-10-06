package com.transnacala.lory;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.transnacala.lory.databinding.ActivityMainBinding;
import com.transnacala.lory.repository.EnqueteRepository;
import com.transnacala.lory.repository.EventoRepository;
import com.transnacala.lory.repository.GrupoRepository;
import com.transnacala.lory.ui.fragment.HomeFragment;
import com.transnacala.lory.ui.fragment.PollsFragment;
import com.transnacala.lory.ui.fragment.ProfileFragment;
import com.transnacala.lory.ui.fragment.TasksFragment;
import com.transnacala.lory.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private EventoRepository eventoRepository;
    private EnqueteRepository enqueteRepository;
    private GrupoRepository grupoRepository;
    private SessionManager sessionManager;
    private int currentTab = 1; // 1: Home, 2: Tasks, 3: Polls, 4: Profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize repositories & session
        eventoRepository = new EventoRepository(this);
        enqueteRepository = new EnqueteRepository(this);
        grupoRepository = new GrupoRepository(this);
        sessionManager = new SessionManager(this);

        // Set default fragment
        if (savedInstanceState == null) {
            switchFragment(new HomeFragment(), 1);
        }

        setupNavigation();
    }

    private void setupNavigation() {
        binding.navHome.setOnClickListener(v -> switchFragment(new HomeFragment(), 1));
        binding.navTasks.setOnClickListener(v -> switchFragment(new TasksFragment(), 2));
        binding.navPolls.setOnClickListener(v -> switchFragment(new PollsFragment(), 3));
        binding.navProfile.setOnClickListener(v -> switchFragment(new ProfileFragment(), 4));

        binding.fabAdd.setOnClickListener(v -> showCreateOptionsDialog());
    }

    private void switchFragment(Fragment fragment, int tabIndex) {
        currentTab = tabIndex;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        updateTabState();
    }

    private void updateTabState() {
        int primaryDarkBlue = ContextCompat.getColor(this, R.color.primary_dark_blue);
        int textSecondary = ContextCompat.getColor(this, R.color.text_secondary);

        // Reset all tabs
        setTabStyle(binding.icNavHome, binding.tvNavHome, false, textSecondary);
        setTabStyle(binding.icNavTasks, binding.tvNavTasks, false, textSecondary);
        setTabStyle(binding.icNavPolls, binding.tvNavPolls, false, textSecondary);
        setTabStyle(binding.icNavProfile, binding.tvNavProfile, false, textSecondary);

        // Highlight selected tab
        switch (currentTab) {
            case 1:
                setTabStyle(binding.icNavHome, binding.tvNavHome, true, primaryDarkBlue);
                break;
            case 2:
                setTabStyle(binding.icNavTasks, binding.tvNavTasks, true, primaryDarkBlue);
                break;
            case 3:
                setTabStyle(binding.icNavPolls, binding.tvNavPolls, true, primaryDarkBlue);
                break;
            case 4:
                setTabStyle(binding.icNavProfile, binding.tvNavProfile, true, primaryDarkBlue);
                break;
        }
    }

    private void setTabStyle(ImageView icon, TextView text, boolean isSelected, int color) {
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setTextColor(color);
        text.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void showCreateOptionsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_options, binding.getRoot(), false);
        dialog.setContentView(dialogView);

        dialogView.findViewById(R.id.btn_create_event).setOnClickListener(v -> {
            dialog.dismiss();
            eventoRepository.insertEvento("c1", "TESTE", "Novo Teste de Engenharia de Software", "2026-10-25T09:00:00");
            Toast.makeText(this, "Evento salvo localmente e agendado para sincronização!", Toast.LENGTH_SHORT).show();
        });

        dialogView.findViewById(R.id.btn_create_poll).setOnClickListener(v -> {
            dialog.dismiss();
            if (!sessionManager.isChefe()) {
                Toast.makeText(this, "Apenas o Chefe de Turma pode criar enquetes.", Toast.LENGTH_SHORT).show();
                return;
            }
            enqueteRepository.insertEnquete(sessionManager.getTurmaId(), "Qual o melhor dia para a palestra sobre IA?", "2026-10-18", sessionManager.getUserName());
            Toast.makeText(this, "Enquete criada localmente e agendada para sincronização!", Toast.LENGTH_SHORT).show();
        });

        dialogView.findViewById(R.id.btn_create_group).setOnClickListener(v -> {
            dialog.dismiss();
            grupoRepository.insertGrupo("c1", "Grupo 3 - Redes & IoT", "Automação Residencial com Esp32");
            Toast.makeText(this, "Grupo criado localmente e agendado para sincronização!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}