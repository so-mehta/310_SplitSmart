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

/** Sign-up screen (SD1): name, USC email, USC ID and password. */
public class SignUpActivity extends AppCompatActivity {

    private SignUpViewModel viewModel;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText uscIdInput;
    private TextInputEditText passwordInput;
    private Button signUpButton;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        nameInput = findViewById(R.id.et_name);
        emailInput = findViewById(R.id.et_email);
        uscIdInput = findViewById(R.id.et_usc_id);
        passwordInput = findViewById(R.id.et_password);
        signUpButton = findViewById(R.id.btn_sign_up);
        progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(SignUpViewModel.class);
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
            signUpButton.setEnabled(!busy);
        });

        signUpButton.setOnClickListener(v -> viewModel.register(
                textOf(nameInput), textOf(emailInput), textOf(uscIdInput),
                textOf(passwordInput)));
        findViewById(R.id.btn_go_to_login).setOnClickListener(v -> finish());
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    /** After sign-up the user is logged in; clear the login screens from the back stack. */
    private void openGroupList(String userId) {
        Intent intent = new Intent(this, GroupListActivity.class);
        intent.putExtra(GroupListActivity.EXTRA_USER_ID, userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
