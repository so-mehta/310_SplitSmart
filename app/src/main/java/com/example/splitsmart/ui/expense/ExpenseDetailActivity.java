package com.example.splitsmart.ui.expense;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.splitsmart.R;

/**
 * EMPTY SKELETON (F2). Christine owns this screen (C3) and replaces this file.
 * Receives groupId, userId and the member id to name lists as extras
 * (GroupDetailActivity.EXTRA_GROUP_ID, EXTRA_USER_ID, EXTRA_MEMBER_IDS, EXTRA_MEMBER_NAMES),
 * plus an expense id extra that Christine defines.
 */
public class ExpenseDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expense_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
