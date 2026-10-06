package com.transnacala.lory;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.transnacala.lory.data.local.AppDatabase;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.databinding.ActivityMainBinding;
import com.transnacala.lory.repository.EnqueteRepository;
import com.transnacala.lory.repository.EventoRepository;
import com.transnacala.lory.repository.GrupoRepository;
import com.transnacala.lory.sync.SyncManager;
import com.transnacala.lory.ui.fragment.HomeFragment;
import com.transnacala.lory.ui.fragment.PollsFragment;
import com.transnacala.lory.ui.fragment.ProfileFragment;
import com.transnacala.lory.ui.fragment.TasksFragment;
import com.transnacala.lory.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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

        // Disparar Sincronização Inicial (Pull do Servidor para o Room) ao abrir
        SyncManager.enqueueSync(this);

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
        BottomSheetDialog optionsDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_options, binding.getRoot(), false);
        optionsDialog.setContentView(dialogView);

        dialogView.findViewById(R.id.btn_create_event).setOnClickListener(v -> {
            optionsDialog.dismiss();
            showAddEventDialog();
        });

        dialogView.findViewById(R.id.btn_create_poll).setOnClickListener(v -> {
            optionsDialog.dismiss();
            if (!sessionManager.isChefe()) {
                Toast.makeText(this, "Apenas o Chefe de Turma pode criar enquetes.", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddPollDialog();
        });

        dialogView.findViewById(R.id.btn_create_group).setOnClickListener(v -> {
            optionsDialog.dismiss();
            showAddGroupDialog();
        });

        optionsDialog.show();
    }

    private void showAddEventDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_event, binding.getRoot(), false);
        dialog.setContentView(view);

        EditText etTitle = view.findViewById(R.id.et_event_title);
        EditText etDate = view.findViewById(R.id.et_event_date);
        Spinner spType = view.findViewById(R.id.sp_event_type);
        Spinner spCadeira = view.findViewById(R.id.sp_event_cadeira);

        // Types Spinner
        String[] types = new String[]{"TESTE", "APRESENTACAO", "OUTRO"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        // Cadeiras Spinner from Room
        List<CadeiraEntity> cadeirasList = new ArrayList<>();
        List<String> cadeirasNames = new ArrayList<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
            List<CadeiraEntity> list = db.cadeiraDao().getCadeirasPendentes();
            if (list != null && !list.isEmpty()) {
                cadeirasList.addAll(list);
                for (CadeiraEntity c : list) {
                    cadeirasNames.add(c.nome);
                }
            } else {
                cadeirasNames.add("Engenharia de Software II");
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> cadAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, cadeirasNames);
                cadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCadeira.setAdapter(cadAdapter);
            });
        });

        view.findViewById(R.id.btn_cancel_event).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_save_event).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String selectedType = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "TESTE";

            if (title.isEmpty()) {
                etTitle.setError("Informe o título");
                return;
            }

            if (date.isEmpty()) {
                date = "2026-11-15 10:00:00";
            }

            String cadeiraId = "cad-01";
            int selectedPos = spCadeira.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < cadeirasList.size()) {
                cadeiraId = cadeirasList.get(selectedPos).id;
            }

            eventoRepository.insertEvento(cadeiraId, selectedType, title, date);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Evento criado com sucesso!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showAddPollDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_poll, binding.getRoot(), false);
        dialog.setContentView(view);

        EditText etQuestion = view.findViewById(R.id.et_poll_question);
        EditText etDeadline = view.findViewById(R.id.et_poll_deadline);

        view.findViewById(R.id.btn_cancel_poll).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_save_poll).setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();

            if (question.isEmpty()) {
                etQuestion.setError("Informe a pergunta");
                return;
            }

            if (deadline.isEmpty()) {
                deadline = "2026-11-20 23:59:59";
            } else if (!deadline.contains(" ")) {
                deadline += " 23:59:59";
            }

            String turmaId = sessionManager.getTurmaId();
            if (turmaId == null || turmaId.isEmpty()) {
                turmaId = "turma-inf-2026";
            }

            enqueteRepository.insertEnquete(turmaId, question, deadline, sessionManager.getUserName());
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Enquete criada com sucesso!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showAddGroupDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_group, binding.getRoot(), false);
        dialog.setContentView(view);

        EditText etName = view.findViewById(R.id.et_group_name);
        EditText etTheme = view.findViewById(R.id.et_group_theme);

        view.findViewById(R.id.btn_cancel_group).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_save_group).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String theme = etTheme.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Informe o nome do grupo");
                return;
            }

            String cadeiraId = "cad-01";
            grupoRepository.insertGrupo(cadeiraId, name, theme.isEmpty() ? "Tema Geral" : theme);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}