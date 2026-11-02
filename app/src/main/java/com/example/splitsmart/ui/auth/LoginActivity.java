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
import com.example.splitsmart.ui.group.GroupListActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Login screen (SD1). Launcher activity. If somebody is already logged in it skips straight to
 * the group list (K12).
 */
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        emailInput = findViewById(R.id.et_email);
        passwordInput = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.user.observe(this, user -> {
            if (user != null) {
                openGroupList(user.getUserId());
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
            loginButton.setEnabled(!busy);
        });

        loginButton.setOnClickListener(v -> viewModel.login(
                textOf(emailInput), textOf(passwordInput)));
        findViewById(R.id.btn_go_to_sign_up).setOnClickListener(
                v -> startActivity(new Intent(this, SignUpActivity.class)));

        if (savedInstanceState == null) {
            viewModel.checkCurrentUser();
        }
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private void openGroupList(String userId) {
        Intent intent = new Intent(this, GroupListActivity.class);
        intent.putExtra(GroupListActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
