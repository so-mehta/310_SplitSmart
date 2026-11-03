package com.example.splitsmart.ui.history;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.model.*;
import com.example.splitsmart.ui.common.MoneyFormatter;
import com.example.splitsmart.ui.expense.*;
import java.util.*;

/** Host passes the group/member snapshot after its shared ViewModel loads it. */
public class HistoryFragment extends Fragment {
    private HistoryViewModel vm;
    private final LinkedHashMap<String, String> members = new LinkedHashMap<>();
    private String groupId, userId;
    private TextView status;
    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private boolean binding;
    private boolean filtersVisible;
    private final ArrayList<Spinner> filterControls = new ArrayList<>();

    public static Bundle arguments(String group, String user, LinkedHashMap<String, String> names) {
        Bundle args = new Bundle(); args.putString(AddEditExpenseActivity.GROUP_ID, group); args.putString(AddEditExpenseActivity.USER_ID, user);
        args.putStringArrayList(AddEditExpenseActivity.MEMBER_IDS, new ArrayList<>(names.keySet()));
        args.putStringArrayList(AddEditExpenseActivity.MEMBER_NAMES, new ArrayList<>(names.values())); return args;
    }
    public static HistoryFragment newInstance(String group, String user, LinkedHashMap<String, String> names) {
        HistoryFragment fragment = new HistoryFragment(); fragment.setArguments(arguments(group, user, names)); return fragment;
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        Bundle args = requireArguments();
        groupId = args.getString(AddEditExpenseActivity.GROUP_ID); userId = args.getString(AddEditExpenseActivity.USER_ID);
        ArrayList<String> ids = args.getStringArrayList(AddEditExpenseActivity.MEMBER_IDS), names = args.getStringArrayList(AddEditExpenseActivity.MEMBER_NAMES);
        if (groupId == null || userId == null || ids == null || names == null || ids.size() != names.size()) throw new IllegalArgumentException("Missing group context.");
        members.clear(); for (int i = 0; i < ids.size(); i++) members.put(ids.get(i), names.get(i));
        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, saved) {
            @NonNull @Override protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> type, @NonNull SavedStateHandle state) {
                return type.cast(new HistoryViewModel(((SplitSmartApp) requireActivity().getApplication()).getHistoryService(), state));
            }
        }).get(HistoryViewModel.class);
        vm.initialize(groupId);
        LinearLayout body = new LinearLayout(requireContext()); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(20,12,20,12);
        TextView heading = new TextView(requireContext()); heading.setText("Group history"); heading.setTextSize(24); body.addView(heading);
        EditText search = new EditText(requireContext()); search.setSingleLine(true); search.setHint("Search titles and notes"); search.setText(vm.text("keyword")); body.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { vm.set("keyword", s.toString()); }
            public void afterTextChanged(Editable value) {}
        });
        LinearLayout filters = new LinearLayout(requireContext()); filters.setOrientation(LinearLayout.VERTICAL);
        filtersVisible = saved != null && saved.getBoolean("filtersVisible");
        Button toggleFilters = new Button(requireContext()); toggleFilters.setText("Show filters"); body.addView(toggleFilters);
        filters.setVisibility(filtersVisible ? View.VISIBLE : View.GONE);
        toggleFilters.setText(filtersVisible ? "Hide filters" : "Show filters");
        toggleFilters.setOnClickListener(v -> {
            filtersVisible = !filtersVisible; filters.setVisibility(filtersVisible ? View.VISIBLE : View.GONE);
            toggleFilters.setText(filtersVisible ? "Hide filters" : "Show filters");
        });
        body.addView(filters);
        binding = true; filterControls.clear();
        selector(filters, "Transaction type", "type", Arrays.asList("", "EXPENSE", "SETTLEMENT"), Arrays.asList("All transactions", "Expenses", "Settlements"));
        ArrayList<String> categories = new ArrayList<>(Collections.singletonList("")), categoryLabels = new ArrayList<>(Collections.singletonList("All categories"));
        for (Category c : Category.values()) { categories.add(c.name()); categoryLabels.add(c.name().replace('_', ' ')); }
        selector(filters, "Category", "category", categories, categoryLabels);
        ArrayList<String> memberKeys = new ArrayList<>(Collections.singletonList("")), memberLabels = new ArrayList<>(Collections.singletonList("All members"));
        memberKeys.addAll(members.keySet()); memberLabels.addAll(members.values());
        selector(filters, "Member", "member", memberKeys, memberLabels);
        selector(filters, "Date order", "order", Arrays.asList("NEWEST_FIRST", "OLDEST_FIRST"), Arrays.asList("Newest first", "Oldest first"));
        Button clear = new Button(requireContext()); clear.setText("Clear search and filters"); filters.addView(clear);
        clear.setOnClickListener(v -> {
            binding = true; vm.clearFilters(); search.setText("");
            for (Spinner control : filterControls) control.setSelection(0);
            binding = false;
        });
        binding = false;
        Button add = new Button(requireContext()); add.setText("Add expense"); body.addView(add);
        add.setOnClickListener(v -> startActivity(AddEditExpenseActivity.intent(requireContext(), groupId, userId, members, null)));
        Button refresh = new Button(requireContext()); refresh.setText("Refresh"); body.addView(refresh); refresh.setOnClickListener(v -> vm.refresh());
        status = new TextView(requireContext()); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(status);
        RecyclerView list = new RecyclerView(requireContext()); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return transactions.size(); }
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                TextView row = new TextView(parent.getContext());
                row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
                row.setTextSize(16); row.setPadding(8,18,8,18);
                return new RecyclerView.ViewHolder(row) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView row = (TextView) holder.itemView;
                Transaction t = transactions.get(position);
                String text;
                if (t instanceof Expense) {
                    Expense e = (Expense) t;
                    LinkedHashSet<String> involved = new LinkedHashSet<>();
                    involved.add(e.getPayerId()); involved.addAll(e.getParticipantIds());
                    ArrayList<String> involvedNames = new ArrayList<>();
                    for (String member : involved) involvedNames.add(name(member));
                    text = "Expense: " + e.getTitle() + "\n" + name(e.getPayerId()) + " paid " + MoneyFormatter.format(e.getAmountCents()) +
                            "\nInvolved: " + String.join(", ", involvedNames);
                } else {
                    Settlement s = (Settlement) t;
                    text = "Settlement: " + name(s.getPayerId()) + " paid " + name(s.getReceiverId()) + " " + MoneyFormatter.format(s.getAmountCents());
                }
                row.setText(text + "\n" + t.getDate());
                row.setOnClickListener(v -> {
                    if (t instanceof Expense) startActivity(ExpenseDetailActivity.intent(requireContext(), groupId, userId, members, t.getTransactionId()));
                    else showSettlement((Settlement) t);
                });
            }
        };
        list.setAdapter(adapter);
        return body;
    }

    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        vm.rows.observe(getViewLifecycleOwner(), rows -> { transactions.clear(); if (rows != null) transactions.addAll(rows); adapter.notifyDataSetChanged(); renderStatus(); });
        vm.error.observe(getViewLifecycleOwner(), message -> renderStatus());
        vm.busy.observe(getViewLifecycleOwner(), value -> renderStatus());
    }
    @Override public void onResume() { super.onResume(); if (vm != null) vm.refresh(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) { out.putBoolean("filtersVisible", filtersVisible); super.onSaveInstanceState(out); }
    private String name(String id) { return members.containsKey(id) ? members.get(id) : "Unknown member"; }
    private void renderStatus() {
        status.setText(Boolean.TRUE.equals(vm.busy.getValue()) ? "Loading..." : vm.error.getValue() != null ? vm.error.getValue() :
                transactions.isEmpty() ? (vm.hasActiveFilters() ? "No transactions found" : "No transactions yet.") : "");
    }
    private void showSettlement(Settlement settlement) {
        String note = settlement.getNote();
        new AlertDialog.Builder(requireContext()).setTitle("Settlement")
                .setMessage("Paid by: " + name(settlement.getPayerId()) +
                        "\nReceived by: " + name(settlement.getReceiverId()) +
                        "\nAmount: " + MoneyFormatter.format(settlement.getAmountCents()) +
                        "\nDate: " + settlement.getDate() +
                        "\nCreated by: " + name(settlement.getCreatorId()) +
                        "\nNote: " + (note == null || note.isEmpty() ? "None" : note))
                .setPositiveButton("Close", null).show();
    }
    private void selector(LinearLayout body, String label, String key, List<String> values, List<String> labels) {
        TextView title = new TextView(requireContext()); title.setText(label); body.addView(title);
        Spinner spinner = new Spinner(requireContext()); spinner.setContentDescription(label);
        filterControls.add(spinner);
        ArrayAdapter<String> choices = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(choices);
        spinner.setSelection(Math.max(0, values.indexOf(vm.text(key)))); body.addView(spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = values.get(position);
                if (!binding && !value.equals(vm.text(key)) && !(vm.text(key).isEmpty() && "NEWEST_FIRST".equals(value))) vm.set(key, value);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    @Override public void onDestroyView() { super.onDestroyView(); adapter = null; status = null; filterControls.clear(); }
}
