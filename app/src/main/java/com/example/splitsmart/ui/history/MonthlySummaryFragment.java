package com.example.splitsmart.ui.history;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.model.Category;
import com.example.splitsmart.ui.common.MoneyFormatter;
import com.example.splitsmart.ui.expense.AddEditExpenseActivity;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MonthlySummaryFragment extends Fragment {
    private MonthlySummaryViewModel vm;
    private Button month;
    private TextView total, categories, status;

    public static MonthlySummaryFragment newInstance(String group, String user, LinkedHashMap<String, String> names) {
        MonthlySummaryFragment fragment = new MonthlySummaryFragment(); fragment.setArguments(HistoryFragment.arguments(group, user, names)); return fragment;
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        String group = requireArguments().getString(AddEditExpenseActivity.GROUP_ID);
        if (group == null) throw new IllegalArgumentException("Missing group context.");
        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, saved) {
            @NonNull @Override protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> type, @NonNull SavedStateHandle state) {
                return type.cast(new MonthlySummaryViewModel(((SplitSmartApp) requireActivity().getApplication()).getSummaryService(), state));
            }
        }).get(MonthlySummaryViewModel.class);
        vm.initialize(group);
        LinearLayout body = new LinearLayout(requireContext()); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(24,24,24,24);
        ScrollView scroll = new ScrollView(requireContext()); scroll.addView(body);
        TextView heading = new TextView(requireContext()); heading.setText("Monthly summary"); heading.setTextSize(24); body.addView(heading);
        month = new Button(requireContext()); body.addView(month); month.setOnClickListener(v -> pickMonth());
        Button previous = new Button(requireContext()); previous.setText("Previous month"); body.addView(previous); previous.setOnClickListener(v -> select(vm.month().minusMonths(1)));
        Button next = new Button(requireContext()); next.setText("Next month"); body.addView(next); next.setOnClickListener(v -> select(vm.month().plusMonths(1)));
        total = new TextView(requireContext()); total.setTextSize(22); body.addView(total);
        categories = new TextView(requireContext()); categories.setTextSize(18); body.addView(categories);
        status = new TextView(requireContext()); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(status);
        Button refresh = new Button(requireContext()); refresh.setText("Refresh"); body.addView(refresh); refresh.setOnClickListener(v -> vm.refresh());
        updateMonth();
        return scroll;
    }
    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        vm.summary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) { total.setText(""); categories.setText(""); return; }
            total.setText(summary.getExpenseCount() + " expenses\n" + MoneyFormatter.format(summary.getTotalCents()));
            StringBuilder text = new StringBuilder();
            for (Category c : Category.values()) text.append(c.name().replace('_', ' ')).append(": ")
                    .append(MoneyFormatter.format(summary.getCategoryTotals().getOrDefault(c, 0L))).append("\n");
            categories.setText(text);
        });
        vm.error.observe(getViewLifecycleOwner(), value -> renderStatus());
        vm.busy.observe(getViewLifecycleOwner(), value -> renderStatus());
    }
    private void renderStatus() { status.setText(Boolean.TRUE.equals(vm.busy.getValue()) ? "Loading..." : vm.error.getValue() == null ? "" : vm.error.getValue()); }
    @Override public void onResume() { super.onResume(); if (vm != null) vm.refresh(); }
    private void select(YearMonth value) {
        if (value.getYear() < 1 || value.getYear() > 9999) return;
        vm.month(value); updateMonth();
    }
    private void updateMonth() { month.setText(vm.month().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))); }
    private void pickMonth() {
        LinearLayout picker = new LinearLayout(requireContext());
        NumberPicker monthPicker = new NumberPicker(requireContext()); monthPicker.setMinValue(1); monthPicker.setMaxValue(12); monthPicker.setValue(vm.month().getMonthValue());
        monthPicker.setDisplayedValues(new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
        NumberPicker yearPicker = new NumberPicker(requireContext()); yearPicker.setMinValue(1); yearPicker.setMaxValue(9999); yearPicker.setValue(vm.month().getYear());
        picker.addView(monthPicker); picker.addView(yearPicker);
        new AlertDialog.Builder(requireContext()).setTitle("Choose month").setView(picker).setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> select(YearMonth.of(yearPicker.getValue(), monthPicker.getValue()))).show();
    }
    @Override public void onDestroyView() { super.onDestroyView(); month = null; total = null; categories = null; status = null; }
}
