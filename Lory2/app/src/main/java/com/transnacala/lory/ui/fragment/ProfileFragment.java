package com.transnacala.lory.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.transnacala.lory.R;
import com.transnacala.lory.databinding.FragmentProfileBinding;
import com.transnacala.lory.ui.adapter.GrupoAdapter;
import com.transnacala.lory.ui.viewmodel.ProfileViewModel;
import com.transnacala.lory.data.local.model.GrupoComCadeira;
import com.transnacala.lory.data.remote.model.GroupMemberDto;
import com.transnacala.lory.utils.Constants;

import java.util.ArrayList;
import java.util.List;

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

        GrupoAdapter grupoAdapter = new GrupoAdapter(this::showGroupMembers);
        binding.rvGrupos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvGrupos.setAdapter(grupoAdapter);
        viewModel.getGruposLiveData().observe(getViewLifecycleOwner(), grupos -> {
            boolean hasGroups = grupos != null && !grupos.isEmpty();
            binding.rvGrupos.setVisibility(hasGroups ? View.VISIBLE : View.GONE);
            binding.tvEmptyGroups.setVisibility(hasGroups ? View.GONE : View.VISIBLE);
            grupoAdapter.setGrupos(grupos);
        });

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

    private void showGroupMembers(GrupoComCadeira group) {
        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        if (Constants.SYNC_STATUS_PENDING_INSERT.equals(group.grupo.syncStatus)) {
            Toast.makeText(requireContext(), "Este grupo precisa sincronizar antes de gerir membros.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        boolean canManage = viewModel.getSessionManager().isChefe();
        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("A carregar estudantes inscritos...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        viewModel.getGrupoRepository().getMembros(group.grupo.id, new retrofit2.Callback<List<GroupMemberDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<GroupMemberDto>> call,
                                   retrofit2.Response<List<GroupMemberDto>> response) {
                loadingDialog.dismiss();
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Não foi possível carregar os estudantes do grupo.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                List<GroupMemberDto> students = response.body();
                CharSequence[] names = new CharSequence[students.size()];
                boolean[] checked = new boolean[students.size()];
                for (int i = 0; i < students.size(); i++) {
                    GroupMemberDto student = students.get(i);
                    names[i] = student.nome;
                    checked[i] = group.grupo.id.equals(student.grupoId);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                        .setTitle("Membros — " + group.grupo.nome);
                if (!canManage) {
                    StringBuilder members = new StringBuilder();
                    for (GroupMemberDto student : students) {
                        if (group.grupo.id.equals(student.grupoId)) {
                            if (members.length() > 0) {
                                members.append('\n');
                            }
                            members.append("• ").append(student.nome);
                        }
                    }
                    builder.setMessage(members.length() == 0 ? "Este grupo ainda não tem membros." : members);
                } else if (students.isEmpty()) {
                    builder.setMessage("Não há estudantes inscritos nesta cadeira.");
                } else {
                    builder.setMultiChoiceItems(names, checked, (dialog, index, selected) -> {
                        GroupMemberDto student = students.get(index);
                        boolean alreadyInAnotherGroup = student.grupoId != null
                                && !group.grupo.id.equals(student.grupoId);
                        if (alreadyInAnotherGroup && selected) {
                            ((AlertDialog) dialog).getListView().setItemChecked(index, false);
                            Toast.makeText(requireContext(),
                                    "Este estudante já pertence a outro grupo desta cadeira.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            checked[index] = selected;
                        }
                    });
                }
                builder.setNegativeButton("Fechar", null);
                if (canManage) {
                    builder.setPositiveButton("Guardar", (dialog, which) -> {
                        List<String> memberIds = new ArrayList<>();
                        for (int i = 0; i < students.size(); i++) {
                            if (checked[i]) {
                                memberIds.add(students.get(i).id);
                            }
                        }
                        viewModel.getGrupoRepository().atualizarMembros(group.grupo.id, memberIds,
                                new retrofit2.Callback<java.util.Map<String, Object>>() {
                                    @Override
                                    public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call,
                                                          retrofit2.Response<java.util.Map<String, Object>> result) {
                                        if (!isAdded()) {
                                            return;
                                        }
                                        String message = result.isSuccessful()
                                                ? "Membros do grupo atualizados."
                                                : result.code() == 409
                                                ? "Um estudante já foi associado a outro grupo desta cadeira."
                                                : "Não foi possível guardar os membros.";
                                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call,
                                                          Throwable error) {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(),
                                                    "Sem ligação ao servidor; não foi possível guardar os membros.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    });
                }
                builder.show();
            }

            @Override
            public void onFailure(retrofit2.Call<List<GroupMemberDto>> call, Throwable error) {
                loadingDialog.dismiss();
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Sem ligação ao servidor; os membros não estão disponíveis offline.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}