package com.example.splitsmart.ui.expense;

import android.content.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.*;
import androidx.savedstate.SavedStateRegistryOwner;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.model.*;
import java.util.*;

/** Native Android form. GroupDetail supplies member IDs/names; this screen never fetches users. */
public class AddEditExpenseActivity extends AppCompatActivity {
    public static final String GROUP_ID = "groupId", USER_ID = "userId", EXPENSE_ID = "expenseId";
    public static final String MEMBER_IDS = "memberIds", MEMBER_NAMES = "memberNames";
    private AddEditExpenseViewModel vm;
    private final LinkedHashMap<String, String> members = new LinkedHashMap<>();
    private LinearLayout form, splits;
    private EditText title, amount, note;
    private Spinner category, payer, method;
    private TextView preview, error, status;
    private Button save, retry;
    private boolean binding;

    public static Intent intent(Context context, String groupId, String userId,
                                LinkedHashMap<String, String> members, String expenseId) {
        return new Intent(context, AddEditExpenseActivity.class)
                .putExtra(GROUP_ID, groupId).putExtra(USER_ID, userId).putExtra(EXPENSE_ID, expenseId)
                .putStringArrayListExtra(MEMBER_IDS, new ArrayList<>(members.keySet()))
                .putStringArrayListExtra(MEMBER_NAMES, new ArrayList<>(members.values()));
    }

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String groupId = getIntent().getStringExtra(GROUP_ID), userId = getIntent().getStringExtra(USER_ID);
        ArrayList<String> ids = getIntent().getStringArrayListExtra(MEMBER_IDS);
        ArrayList<String> names = getIntent().getStringArrayListExtra(MEMBER_NAMES);
        if (groupId == null || userId == null || ids == null || names == null || ids.size() != names.size()) {
            Toast.makeText(this, "Missing group or member information.", Toast.LENGTH_LONG).show(); finish(); return;
        }
        for (int i = 0; i < ids.size(); i++) members.put(ids.get(i), names.get(i));
        vm = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory((SavedStateRegistryOwner) this, saved) {
            @NonNull @Override protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> type, @NonNull SavedStateHandle handle) {
                return type.cast(new AddEditExpenseViewModel(((SplitSmartApp) getApplication()).getExpenseService(), handle));
            }
        }).get(AddEditExpenseViewModel.class);
        buildForm();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmLeave(); }
        });
        vm.expenseToLoad.observe(this, expense -> { if (expense != null) vm.applyLoadedExpense(expense); });
        vm.loaded.observe(this, ready -> {
            if (Boolean.TRUE.equals(ready)) bindDraft();
            updateEnabled();
        });
        vm.previewText.observe(this, preview::setText);
        vm.valid.observe(this, value -> updateEnabled());
        vm.busy.observe(this, value -> { status.setText(Boolean.TRUE.equals(value) ? "Please wait..." : ""); updateEnabled(); });
        vm.error.observe(this, message -> { error.setText(message == null ? "" : message); retry.setVisibility(Boolean.TRUE.equals(vm.loaded.getValue()) ? View.GONE : View.VISIBLE); });
        vm.saved.observe(this, done -> { if (Boolean.TRUE.equals(done)) { setResult(RESULT_OK); finish(); } });
        vm.initialize(groupId, userId, getIntent().getStringExtra(EXPENSE_ID), members);
    }

    private void buildForm() {
        ScrollView scroll = new ScrollView(this);
        form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(24, 24, 24, 32);
        scroll.setFillViewport(true); scroll.addView(form); setContentView(scroll);
        label(getIntent().getStringExtra(EXPENSE_ID) == null ? "Add expense" : "Edit expense").setTextSize(26);
        label("Title"); title = field("Dinner, groceries, rent...", false);
        label("Total amount ($)"); amount = field("0.00", true);
        label("Category"); category = spinner(Category.values());
        label("Paid by"); payer = spinner(members.values().toArray());
        label("Note (optional)"); note = field("Add a note", false);
        label("Split between at least two members");
        for (Map.Entry<String, String> entry : members.entrySet()) {
            CheckBox check = new CheckBox(this); check.setText(entry.getValue()); check.setTag(entry.getKey()); form.addView(check);
            check.setOnCheckedChangeListener((button, selected) -> {
                if (!binding) { vm.select(entry.getKey(), selected); renderInputs(); }
            });
        }
        label("Split method"); method = spinner(SplitMethod.values());
        splits = new LinearLayout(this); splits.setOrientation(LinearLayout.VERTICAL); form.addView(splits);
        label("Split preview").setTextSize(20);
        preview = label(""); preview.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        error = label(""); error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status = label("");
        retry = new Button(this); retry.setText("Retry loading"); retry.setVisibility(View.GONE); form.addView(retry); retry.setOnClickListener(v -> vm.load());
        save = new Button(this); save.setText("Save expense"); save.setEnabled(false); form.addView(save); save.setOnClickListener(v -> vm.save());
        Button cancel = new Button(this); cancel.setText("Cancel"); form.addView(cancel); cancel.setOnClickListener(v -> confirmLeave());
        watch(title, "title"); watch(amount, "amount"); watch(note, "note");
        onSelect(category, () -> vm.setText("category", Category.values()[category.getSelectedItemPosition()].name()));
        onSelect(payer, () -> {
            if (!members.isEmpty()) {
                vm.setText("payer", new ArrayList<>(members.keySet()).get(payer.getSelectedItemPosition()));
                bindParticipants(); renderInputs();
            }
        });
        onSelect(method, () -> { vm.setText("method", SplitMethod.values()[method.getSelectedItemPosition()].name()); renderInputs(); });
    }

    private void bindDraft() {
        binding = true;
        title.setText(vm.text("title")); amount.setText(vm.text("amount")); note.setText(vm.text("note"));
        if (vm.text("category").isEmpty()) vm.setText("category", Category.OTHER.name());
        if (vm.text("payer").isEmpty() && !members.isEmpty()) vm.setText("payer", members.keySet().iterator().next());
        if (!vm.text("payer").isEmpty()) vm.select(vm.text("payer"), true);
        category.setSelection(Category.valueOf(vm.text("category")).ordinal());
        int index = new ArrayList<>(members.keySet()).indexOf(vm.text("payer"));
        payer.setSelection(Math.max(0, index)); method.setSelection(vm.method().ordinal());
        bindParticipants();
        binding = false; renderInputs(); vm.refreshPreview();
    }

    private void bindParticipants() {
        boolean wasBinding = binding; binding = true;
        for (int i = 0; i < form.getChildCount(); i++) {
            View child = form.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox check = (CheckBox) child;
                check.setChecked(vm.participants().contains((String) check.getTag()));
                check.setEnabled(!check.getTag().equals(vm.text("payer")));
            }
        }
        binding = wasBinding;
    }

    private void confirmLeave() {
        if (Boolean.TRUE.equals(vm.busy.getValue())) {
            new AlertDialog.Builder(this).setMessage("Please wait for the current operation to finish.").setPositiveButton("OK", null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Leave expense form?")
                .setMessage("Your unsaved changes will be discarded.")
                .setNegativeButton("Keep editing", null).setPositiveButton("Discard", (dialog, which) -> finish()).show();
    }

    private void renderInputs() {
        splits.removeAllViews();
        if (vm.method() == SplitMethod.EQUAL) return;
        for (String id : members.keySet()) {
            if (!vm.participants().contains(id)) continue;
            TextView name = new TextView(this); name.setText(members.get(id) + (vm.method() == SplitMethod.PERCENTAGE ? " (%)" : " ($)")); splits.addView(name);
            EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(8194);
            input.setContentDescription(name.getText()); input.setText(vm.input(id)); splits.addView(input);
            input.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) { if (!binding) vm.input(id, s.toString()); }
                public void afterTextChanged(Editable value) {}
            });
        }
    }

    private void updateEnabled() {
        boolean enabled = Boolean.TRUE.equals(vm.loaded.getValue()) && !Boolean.TRUE.equals(vm.busy.getValue());
        setEnabled(form, enabled);
        if (enabled) bindParticipants();
        retry.setEnabled(!Boolean.TRUE.equals(vm.busy.getValue()));
        save.setEnabled(enabled && Boolean.TRUE.equals(vm.valid.getValue()));
    }

    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) setEnabled(((ViewGroup) view).getChildAt(i), enabled);
    }

    private TextView label(String text) { TextView view = new TextView(this); view.setText(text); view.setTextSize(16); view.setPadding(0, 12, 0, 4); form.addView(view); return view; }
    private EditText field(String hint, boolean numeric) { EditText view = new EditText(this); view.setHint(hint); view.setSingleLine(true); if (numeric) view.setInputType(8194); form.addView(view); return view; }
    private Spinner spinner(Object[] values) {
        Spinner view = new Spinner(this);
        ArrayList<String> labels = new ArrayList<>();
        for (Object value : values) labels.add(String.valueOf(value).replace('_', ' '));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); view.setAdapter(adapter); form.addView(view); return view;
    }
    private void onSelect(Spinner view, Runnable change) {
        view.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View row, int position, long id) { if (!binding && Boolean.TRUE.equals(vm.loaded.getValue())) change.run(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void watch(EditText view, String key) {
        view.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { if (!binding) vm.setText(key, s.toString()); }
            public void afterTextChanged(Editable value) {}
        });
    }
}
