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
 * EMPTY SKELETON (F3). Christine owns the History tab (C4) and replaces this file. Read the
 * group, members and names from the shared ViewModel:
 * <pre>
 * new ViewModelProvider(requireActivity()).get(GroupDetailViewModel.class)
 * </pre>
 * Reload in onResume so the tab refreshes after an expense or settlement was saved.
 */
public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }
}
