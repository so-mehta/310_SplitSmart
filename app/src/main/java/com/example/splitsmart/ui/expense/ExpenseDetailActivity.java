package com.example.splitsmart.ui.expense;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.*;
import androidx.lifecycle.*;
import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.splitsmart.model.*;
import com.example.splitsmart.ui.common.MoneyFormatter;
import java.util.*;

public class ExpenseDetailActivity extends AppCompatActivity {
    private ExpenseDetailViewModel vm;
    private final LinkedHashMap<String, String> members = new LinkedHashMap<>();
    private String groupId, userId, expenseId;
    private TextView detail, error;
    private Button edit, delete;
    private boolean changed;

    public static Intent intent(Context context, String group, String user, LinkedHashMap<String, String> names, String id) {
        return AddEditExpenseActivity.intent(context, group, user, names, id).setClass(context, ExpenseDetailActivity.class);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        groupId = getIntent().getStringExtra(AddEditExpenseActivity.GROUP_ID);
        userId = getIntent().getStringExtra(AddEditExpenseActivity.USER_ID);
        expenseId = getIntent().getStringExtra(AddEditExpenseActivity.EXPENSE_ID);
        ArrayList<String> ids = getIntent().getStringArrayListExtra(AddEditExpenseActivity.MEMBER_IDS);
        ArrayList<String> names = getIntent().getStringArrayListExtra(AddEditExpenseActivity.MEMBER_NAMES);
        if (groupId == null || userId == null || expenseId == null || ids == null || names == null || ids.size() != names.size()) { finish(); return; }
        for (int i = 0; i < ids.size(); i++) members.put(ids.get(i), names.get(i));
        changed = state != null && state.getBoolean("changed");
        vm = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) { return type.cast(new ExpenseDetailViewModel(((SplitSmartApp) getApplication()).getExpenseService())); }
        }).get(ExpenseDetailViewModel.class);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(24,24,24,24);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expense_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ScrollView scroll = findViewById(R.id.expense_detail_scroll); scroll.addView(body);
        detail = new TextView(this); detail.setTextSize(18); body.addView(detail);
        error = new TextView(this); error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(error);
        edit = new Button(this); edit.setText("Edit expense"); edit.setVisibility(View.GONE); body.addView(edit);
        delete = new Button(this); delete.setText("Delete expense"); delete.setVisibility(View.GONE); body.addView(delete);
        Button retry = new Button(this); retry.setText("Refresh"); body.addView(retry); retry.setOnClickListener(v -> vm.load(groupId, expenseId));
        edit.setOnClickListener(v -> startActivityForResult(AddEditExpenseActivity.intent(this, groupId, userId, members, expenseId), 1));
        delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete expense?")
                .setMessage("Are you sure you want to delete this expense?")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> vm.delete(userId)).show());
        vm.expense.observe(this, this::render);
        vm.error.observe(this, message -> error.setText(message == null ? "" : message));
        vm.busy.observe(this, loading -> { edit.setEnabled(!Boolean.TRUE.equals(loading)); delete.setEnabled(!Boolean.TRUE.equals(loading)); retry.setEnabled(!Boolean.TRUE.equals(loading)); });
        vm.deleted.observe(this, done -> { if (Boolean.TRUE.equals(done)) { setResult(RESULT_OK); finish(); } });
    }

    @Override protected void onResume() { super.onResume(); if (vm != null) vm.load(groupId, expenseId); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 1 && result == RESULT_OK) { changed = true; setResult(RESULT_OK); }
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("changed", changed); super.onSaveInstanceState(out); }

    private String name(String id) { return members.containsKey(id) ? members.get(id) : "Unknown member"; }
    private void render(Expense e) {
        boolean owner = e != null && userId.equals(e.getCreatorId());
        edit.setVisibility(owner ? View.VISIBLE : View.GONE); delete.setVisibility(owner ? View.VISIBLE : View.GONE);
        if (e == null) { detail.setText(""); return; }
        StringBuilder text = new StringBuilder(e.getTitle()).append("\n").append(MoneyFormatter.format(e.getAmountCents()))
                .append("\nCategory: ").append(e.getCategory()).append("\nPaid by: ").append(name(e.getPayerId()))
                .append("\nCreated by: ").append(name(e.getCreatorId())).append("\nDate: ").append(e.getDate())
                .append("\nSplit: ").append(e.getSplitMethod().name().replace('_', ' ')).append("\nNote: ")
                .append(e.getNote() == null || e.getNote().isEmpty() ? "None" : e.getNote()).append("\n\nShares\n");
        for (Share s : e.getShares()) text.append(name(s.getUserId())).append(": ").append(MoneyFormatter.format(s.getShareCents()))
                .append(e.getSplitMethod() == SplitMethod.PERCENTAGE ? " (" + s.getPercent() + "%)" : "").append("\n");
        detail.setText(text);
        if (changed) setResult(RESULT_OK);
    }
}
