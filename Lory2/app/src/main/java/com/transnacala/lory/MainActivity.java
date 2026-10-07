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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

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
import com.transnacala.lory.ui.fragment.EventsFragment;
import com.transnacala.lory.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private EventoRepository eventoRepository;
    private EnqueteRepository enqueteRepository;
    private GrupoRepository grupoRepository;
    private SessionManager sessionManager;
    private int currentTab = 1;

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

        // Refresh now and schedule periodic server reconciliation.
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
        binding.navEvents.setOnClickListener(v -> switchFragment(new EventsFragment(), 3));
        binding.navPolls.setOnClickListener(v -> switchFragment(new PollsFragment(), 4));
        binding.navProfile.setOnClickListener(v -> switchFragment(new ProfileFragment(), 5));

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
        setTabStyle(binding.icNavEvents, binding.tvNavEvents, false, textSecondary);
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
                setTabStyle(binding.icNavEvents, binding.tvNavEvents, true, primaryDarkBlue);
                break;
            case 4:
                setTabStyle(binding.icNavPolls, binding.tvNavPolls, true, primaryDarkBlue);
                break;
            case 5:
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

        List<CadeiraEntity> cadeirasList = observeCadeiras(spCadeira,
                view.findViewById(R.id.tv_event_cadeira_status), dialog);

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
                etDate.setError("Informe a data e hora do evento");
                return;
            }

            int selectedPos = spCadeira.getSelectedItemPosition();
            if (selectedPos < 0 || selectedPos >= cadeirasList.size()) {
                Toast.makeText(MainActivity.this, R.string.event_requires_cadeira, Toast.LENGTH_LONG).show();
                return;
            }

            String cadeiraId = cadeirasList.get(selectedPos).id;
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
        Spinner spCadeira = view.findViewById(R.id.sp_group_cadeira);
        List<CadeiraEntity> cadeirasList = observeCadeiras(spCadeira,
                view.findViewById(R.id.tv_group_cadeira_status), dialog);

        view.findViewById(R.id.btn_cancel_group).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_save_group).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String theme = etTheme.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Informe o nome do grupo");
                return;
            }

            int selectedPos = spCadeira.getSelectedItemPosition();
            if (selectedPos < 0 || selectedPos >= cadeirasList.size()) {
                Toast.makeText(MainActivity.this, R.string.event_requires_cadeira, Toast.LENGTH_LONG).show();
                return;
            }

            String cadeiraId = cadeirasList.get(selectedPos).id;
            grupoRepository.insertGrupo(cadeiraId, name, theme.isEmpty() ? "Tema Geral" : theme);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private List<CadeiraEntity> observeCadeiras(Spinner spinner, TextView status, BottomSheetDialog dialog) {
        List<CadeiraEntity> cadeiras = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, nomes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LiveData<List<CadeiraEntity>> cadeirasLiveData =
                AppDatabase.getInstance(this).cadeiraDao().getCadeirasLiveData();
        Observer<List<CadeiraEntity>> observer = atualizadas -> {
            cadeiras.clear();
            nomes.clear();
            if (atualizadas != null) {
                cadeiras.addAll(atualizadas);
                for (CadeiraEntity cadeira : atualizadas) {
                    nomes.add(cadeira.nome);
                }
            }
            adapter.notifyDataSetChanged();
            status.setVisibility(nomes.isEmpty() ? View.VISIBLE : View.GONE);
        };

        cadeirasLiveData.observe(this, observer);
        dialog.setOnDismissListener(ignored -> cadeirasLiveData.removeObserver(observer));
        return cadeiras;
    }
}