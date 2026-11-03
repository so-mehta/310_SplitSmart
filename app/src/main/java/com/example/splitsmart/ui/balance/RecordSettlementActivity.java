package com.example.splitsmart.ui.balance;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.splitsmart.R;
import com.example.splitsmart.ui.common.MoneyFormatter;
import com.example.splitsmart.ui.group.GroupDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Record a settlement (SD6). A spinner lists the members the user owes (from
 * getOwedMembers), then an amount, an optional note and Save. A ValidationException stays on
 * screen. "You do not owe anyone" shows when the owed map is empty. On success the screen
 * closes with RESULT_OK, and the Balances and History tabs refresh when they are shown again.
 * Receives groupId, userId and the member id to name lists as extras (K12).
 */
public class RecordSettlementActivity extends AppCompatActivity {

    private RecordSettlementViewModel viewModel;
    private Spinner receiverSpinner;
    private TextInputEditText amountInput;
    private TextInputEditText noteInput;
    private Button saveButton;
    private LinearProgressIndicator progress;
    private View formGroup;
    private View nothingOwedView;

    private String groupId;
    private String userId;
    private final Map<String, String> memberNames = new HashMap<>();
    /** Receiver ids in the same order as the spinner rows. */
    private final List<String> receiverIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_settlement);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        groupId = getIntent().getStringExtra(GroupDetailActivity.EXTRA_GROUP_ID);
        userId = getIntent().getStringExtra(GroupDetailActivity.EXTRA_USER_ID);
        readMemberNames();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        receiverSpinner = findViewById(R.id.spinner_receiver);
        amountInput = findViewById(R.id.et_amount);
        noteInput = findViewById(R.id.et_note);
        saveButton = findViewById(R.id.btn_save);
        progress = findViewById(R.id.progress);
        formGroup = findViewById(R.id.form_group);
        nothingOwedView = findViewById(R.id.tv_nothing_owed);

        viewModel = new ViewModelProvider(this).get(RecordSettlementViewModel.class);
        viewModel.owed.observe(this, this::showOwedMembers);
        viewModel.saved.observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        viewModel.error.observe(this, message -> {
            if (message != null) {
                showError(message);
                viewModel.error.setValue(null);
            }
        });
        viewModel.loading.observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
            saveButton.setEnabled(!busy);
        });

        saveButton.setOnClickListener(v -> {
            int position = receiverSpinner.getSelectedItemPosition();
            if (position < 0 || position >= receiverIds.size()) {
                return;
            }
            viewModel.save(groupId, userId, receiverIds.get(position),
                    textOf(amountInput), textOf(noteInput));
        });

        if (savedInstanceState == null) {
            viewModel.loadOwed(groupId, userId);
        }
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private void readMemberNames() {
        ArrayList<String> ids = getIntent().getStringArrayListExtra(
                GroupDetailActivity.EXTRA_MEMBER_IDS);
        ArrayList<String> names = getIntent().getStringArrayListExtra(
                GroupDetailActivity.EXTRA_MEMBER_NAMES);
        if (ids == null || names == null) {
            return;
        }
        for (int i = 0; i < ids.size() && i < names.size(); i++) {
            memberNames.put(ids.get(i), names.get(i));
        }
    }

    private void showOwedMembers(Map<String, Long> owed) {
        if (owed == null) {
            return;
        }
        receiverIds.clear();
        List<String> labels = new ArrayList<>();
        for (Map.Entry<String, Long> entry : owed.entrySet()) {
            String name = memberNames.get(entry.getKey());
            if (name == null) {
                name = getString(R.string.unknown_member);
            }
            receiverIds.add(entry.getKey());
            labels.add(getString(R.string.owed_option, name,
                    MoneyFormatter.format(entry.getValue())));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        receiverSpinner.setAdapter(adapter);

        boolean nobody = owed.isEmpty();
        nothingOwedView.setVisibility(nobody ? View.VISIBLE : View.GONE);
        formGroup.setVisibility(nobody ? View.GONE : View.VISIBLE);
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
