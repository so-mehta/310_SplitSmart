package com.example.splitsmart.ui.balance;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitsmart.R;
import com.example.splitsmart.service.BalanceSummary;
import com.example.splitsmart.ui.common.MoneyFormatter;
import com.example.splitsmart.ui.group.GroupDetailViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Balances tab (SD5). Shows "You owe" and "You are owed" totals, then one line per member such
 * as "You owe Bob $11.00". Members at $0.00 are hidden, and "Settled up" shows when nobody is
 * left. Names come from the shared {@link GroupDetailViewModel} (K12). When the group cannot be
 * read (the user is not a member) the message is shown instead.
 */
public class BalanceFragment extends Fragment {

    private GroupDetailViewModel shared;
    private BalanceViewModel viewModel;
    private BalanceAdapter adapter;

    private TextView messageView;
    private View contentGroup;
    private TextView totalOweView;
    private TextView totalOwedView;
    private View settledUpView;
    private Button recordButton;
    private LinearProgressIndicator progress;

    private String loadErrorMessage;
    private String balanceErrorMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shared = new ViewModelProvider(requireActivity()).get(GroupDetailViewModel.class);
        viewModel = new ViewModelProvider(this).get(BalanceViewModel.class);

        messageView = view.findViewById(R.id.tv_message);
        contentGroup = view.findViewById(R.id.content_group);
        totalOweView = view.findViewById(R.id.tv_total_owe);
        totalOwedView = view.findViewById(R.id.tv_total_owed);
        settledUpView = view.findViewById(R.id.tv_settled_up);
        recordButton = view.findViewById(R.id.btn_record_settlement);
        progress = view.findViewById(R.id.progress);

        adapter = new BalanceAdapter();
        RecyclerView recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        recordButton.setOnClickListener(v -> startActivity(shared.addContextExtras(
                new Intent(requireContext(), RecordSettlementActivity.class))));

        // Load once the group is known. A tab that is not showing yet waits for onResume.
        shared.group.observe(getViewLifecycleOwner(), group -> {
            if (group != null && isResumed()) {
                reload();
            }
        });
        shared.loadError.observe(getViewLifecycleOwner(), message -> {
            loadErrorMessage = message;
            updateMessage();
        });
        shared.memberNames.observe(getViewLifecycleOwner(), names -> render());

        viewModel.summary.observe(getViewLifecycleOwner(), summary -> render());
        viewModel.error.observe(getViewLifecycleOwner(), message -> {
            balanceErrorMessage = message;
            updateMessage();
        });
        viewModel.loading.observe(getViewLifecycleOwner(), loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.INVISIBLE));
    }

    /**
     * Reload whenever the tab is shown again: after a settlement was recorded, or after an
     * expense was added from the History tab.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (shared.group.getValue() != null) {
            reload();
        }
    }

    private void reload() {
        viewModel.load(shared.groupId(), shared.userId());
    }

    /** Shows the failure message in place of the balances, or hides it when there is none. */
    private void showError(String message) {
        boolean failed = message != null;
        messageView.setText(message);
        messageView.setVisibility(failed ? View.VISIBLE : View.GONE);
        contentGroup.setVisibility(failed ? View.GONE : View.VISIBLE);
    }

    private void updateMessage() {
        showError(loadErrorMessage != null ? loadErrorMessage : balanceErrorMessage);
    }

    private void render() {
        BalanceSummary summary = viewModel.summary.getValue();
        Map<String, String> names = shared.memberNames.getValue();
        if (summary == null || names == null) {
            return;
        }
        totalOweView.setText(MoneyFormatter.format(summary.getTotalToPayCents()));
        totalOwedView.setText(MoneyFormatter.format(summary.getTotalToReceiveCents()));

        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : summary.getIndividualBalances().entrySet()) {
            long cents = entry.getValue();
            if (cents == 0) {
                continue; // hide members at $0.00
            }
            String name = names.get(entry.getKey());
            if (name == null) {
                name = getString(R.string.unknown_member);
            }
            String money = MoneyFormatter.format(Math.abs(cents));
            if (cents > 0) {
                lines.add(new Line(name, getString(R.string.balance_owes_you, name, money),
                        ContextCompat.getColor(requireContext(), R.color.owed)));
            } else {
                lines.add(new Line(name, getString(R.string.balance_you_owe, name, money),
                        ContextCompat.getColor(requireContext(), R.color.owe)));
            }
        }
        Collections.sort(lines, (a, b) -> a.name.compareToIgnoreCase(b.name));
        adapter.submit(lines);
        settledUpView.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** One display line: the member's name (for sorting), the text and its color. */
    private static class Line {
        final String name;
        final String text;
        final int color;

        Line(String name, String text, int color) {
            this.name = name;
            this.text = text;
            this.color = color;
        }
    }

    private static class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.Holder> {

        private final List<Line> items = new ArrayList<>();

        void submit(List<Line> lines) {
            items.clear();
            items.addAll(lines);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_balance, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Line line = items.get(position);
            holder.text.setText(line.text);
            holder.text.setTextColor(line.color);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView text;

            Holder(@NonNull View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.tv_balance_line);
            }
        }
    }
}
