package com.example.splitsmart.ui.group;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.splitsmart.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Creates a group (SD2). When started with EXTRA_GROUP_ID it edits that group instead, so the
 * owner can change the name and description from the group detail screen.
 */
public class CreateGroupActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "com.example.splitsmart.extra.EDIT_GROUP_ID";
    public static final String EXTRA_NAME = "com.example.splitsmart.extra.EDIT_GROUP_NAME";
    public static final String EXTRA_DESC = "com.example.splitsmart.extra.EDIT_GROUP_DESC";

    private CreateGroupViewModel viewModel;
    private TextInputEditText nameInput;
    private TextInputEditText descInput;
    private Button saveButton;
    private LinearProgressIndicator progress;
    private String userId;
    private String editGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_group);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra(GroupListActivity.EXTRA_USER_ID);
        editGroupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        boolean editing = editGroupId != null;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        nameInput = findViewById(R.id.et_name);
        descInput = findViewById(R.id.et_desc);
        saveButton = findViewById(R.id.btn_save);
        progress = findViewById(R.id.progress);

        if (editing) {
            toolbar.setTitle(R.string.title_edit_group);
            saveButton.setText(R.string.action_save);
            if (savedInstanceState == null) {
                nameInput.setText(getIntent().getStringExtra(EXTRA_NAME));
                descInput.setText(getIntent().getStringExtra(EXTRA_DESC));
            }
        }

        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);
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
            String name = textOf(nameInput);
            String desc = textOf(descInput);
            if (editing) {
                viewModel.updateGroup(userId, editGroupId, name, desc);
            } else {
                viewModel.createGroup(userId, name, desc);
            }
        });
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
