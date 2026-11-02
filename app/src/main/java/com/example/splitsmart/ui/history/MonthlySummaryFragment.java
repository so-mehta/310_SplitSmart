package com.example.splitsmart.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splitsmart.R;

/**
 * EMPTY SKELETON (F3). Christine owns the Summary tab (C5) and replaces this file. It reads
 * the group id from the shared GroupDetailViewModel (see HistoryFragment).
 */
public class MonthlySummaryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly_summary, container, false);
    }
}
