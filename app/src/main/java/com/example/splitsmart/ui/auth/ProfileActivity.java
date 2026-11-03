package com.example.splitsmart.ui.auth;

import android.content.Intent;
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
 * Profile screen (SD1): read-only email and USC ID, editable name, and Log Out, which clears
 * the back stack.
 */
public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;
    private TextInputEditText emailInput;
    private TextInputEditText uscIdInput;
    private TextInputEditText nameInput;
    private Button saveButton;
    private Button logoutButton;
    private LinearProgressIndicator progress;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emailInput = findViewById(R.id.et_email);
        uscIdInput = findViewById(R.id.et_usc_id);
        nameInput = findViewById(R.id.et_name);
        saveButton = findViewById(R.id.btn_save_name);
        logoutButton = findViewById(R.id.btn_logout);
        progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.user.observe(this, user -> {
            if (user == null) {
                return;
            }
            userId = user.getUserId();
            emailInput.setText(user.getEmail());
            uscIdInput.setText(user.getUscId());
            // Do not overwrite what the user is typing after a rotation.
            if (nameInput.getText() == null || nameInput.getText().length() == 0) {
                nameInput.setText(user.getName());
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
            logoutButton.setEnabled(!busy);
        });
        viewModel.nameSaved.observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                viewModel.nameSaved.setValue(null);
                Snackbar.make(findViewById(R.id.root), R.string.message_name_saved,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        viewModel.loggedOut.observe(this, loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                openLogin();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (userId != null) {
                viewModel.saveName(userId, textOf(nameInput));
            }
        });
        logoutButton.setOnClickListener(v -> viewModel.logout());

        if (savedInstanceState == null) {
            viewModel.load();
        }
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    /** Back to the login screen with an empty back stack. */
    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
