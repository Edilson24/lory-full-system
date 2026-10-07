package com.transnacala.lory.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.lory.R;
import com.transnacala.lory.data.local.entity.VotoLocalEntity;
import com.transnacala.lory.data.local.model.EnqueteComQuestoes;
import com.transnacala.lory.databinding.ItemEnqueteCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnqueteAdapter extends RecyclerView.Adapter<EnqueteAdapter.EnqueteViewHolder> {

    public interface OnVoteListener {
        void onVote(String questaoId, String opcaoId);
    }

    private final List<EnqueteComQuestoes> enquetes = new ArrayList<>();
    private final Map<String, String> votosDoUtilizador = new HashMap<>();
    private final OnVoteListener listener;

    public EnqueteAdapter(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setEnquetes(List<EnqueteComQuestoes> newEnquetes) {
        enquetes.clear();
        if (newEnquetes != null) {
            enquetes.addAll(newEnquetes);
        }
        notifyDataSetChanged();
    }

    public void setVotos(List<VotoLocalEntity> votos) {
        votosDoUtilizador.clear();
        if (votos != null) {
            for (VotoLocalEntity voto : votos) {
                votosDoUtilizador.put(voto.questaoId, voto.opcaoId);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EnqueteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEnqueteCardBinding binding = ItemEnqueteCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EnqueteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EnqueteViewHolder holder, int position) {
        holder.bind(enquetes.get(position), votosDoUtilizador, listener);
    }

    @Override
    public int getItemCount() {
        return enquetes.size();
    }

    static class EnqueteViewHolder extends RecyclerView.ViewHolder {
        private final ItemEnqueteCardBinding binding;

        EnqueteViewHolder(@NonNull ItemEnqueteCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EnqueteComQuestoes item, Map<String, String> votos, OnVoteListener listener) {
            binding.tvEnquetePergunta.setText(item.enquete.pergunta);
            String criador = item.enquete.criadaPor != null ? item.enquete.criadaPor : "Chefe de Turma";
            boolean aberta = isOpen(item.enquete.prazo);
            binding.tvEnqueteMeta.setText("Criado por: " + criador
                    + (aberta ? " | Prazo: " + item.enquete.prazo : " | Enquete encerrada"));
            binding.llPollOptions.removeAllViews();
            if (item.questoes == null) {
                return;
            }

            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            for (com.transnacala.lory.data.local.model.QuestaoComOpcoes question : item.questoes) {
                TextView questionTitle = new TextView(binding.getRoot().getContext());
                questionTitle.setText(question.questao.pergunta);
                questionTitle.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.text_primary));
                questionTitle.setTextSize(15);
                questionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                android.widget.LinearLayout.LayoutParams titleParams =
                        new android.widget.LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                titleParams.topMargin = (int) (8 * density);
                titleParams.bottomMargin = (int) (6 * density);
                binding.llPollOptions.addView(questionTitle, titleParams);

                if (question.opcoes == null) {
                    continue;
                }
                String selectedOption = votos.get(question.questao.id);
                for (com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity option : question.opcoes) {
                    TextView optionView = new TextView(binding.getRoot().getContext());
                    boolean selected = option.id.equals(selectedOption);
                    optionView.setText(option.texto + "  -  " + option.votosCount
                            + (option.votosCount == 1 ? " voto" : " votos")
                            + (selected ? "  -  Seu voto" : ""));
                    optionView.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                            selected ? R.color.white : R.color.text_primary));
                    optionView.setTextSize(14);
                    optionView.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    optionView.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
                    optionView.setBackgroundResource(selected
                            ? R.drawable.bg_card_dark : R.drawable.bg_card_light);
                    optionView.setClickable(aberta);
                    optionView.setFocusable(aberta);
                    optionView.setAlpha(aberta ? 1f : 0.6f);
                    if (aberta) {
                        optionView.setOnClickListener(v ->
                                listener.onVote(question.questao.id, option.id));
                    }
                    android.widget.LinearLayout.LayoutParams optionParams =
                            new android.widget.LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density));
                    optionParams.bottomMargin = (int) (8 * density);
                    binding.llPollOptions.addView(optionView, optionParams);
                }
            }
        }

        private boolean isOpen(String prazo) {
            if (prazo == null || prazo.length() < 19) {
                return false;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            format.setLenient(false);
            try {
                Date deadline = format.parse(prazo.substring(0, 19).replace('T', ' '));
                return deadline != null && deadline.getTime() >= System.currentTimeMillis();
            } catch (ParseException exception) {
                return false;
            }
        }
    }
}
