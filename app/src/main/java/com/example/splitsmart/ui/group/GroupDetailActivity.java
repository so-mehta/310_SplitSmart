package com.example.splitsmart.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.splitsmart.R;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.ui.balance.BalanceFragment;
import com.example.splitsmart.ui.history.HistoryFragment;
import com.example.splitsmart.ui.history.MonthlySummaryFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Container for one group (SD2): a tab bar with History, Balances, Members and Summary.
 * The activity-scoped {@link GroupDetailViewModel} loads the group and its members once and
 * every fragment reads from it (K12). The header shows the group name. Edit and Delete appear
 * for the owner only, and Delete asks for confirmation.
 */
public class GroupDetailActivity extends AppCompatActivity {

    /** Extras (K12). Member ids and names are two lists with the same order. */
    public static final String EXTRA_GROUP_ID = "com.example.splitsmart.extra.GROUP_ID";
    public static final String EXTRA_USER_ID = "com.example.splitsmart.extra.USER_ID";
    public static final String EXTRA_MEMBER_IDS = "com.example.splitsmart.extra.MEMBER_IDS";
    public static final String EXTRA_MEMBER_NAMES = "com.example.splitsmart.extra.MEMBER_NAMES";

    private static final int TAB_COUNT = 4;

    private GroupDetailViewModel viewModel;
    private MaterialToolbar toolbar;

    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    viewModel.load();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        String groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);

        viewModel = new ViewModelProvider(this).get(GroupDetailViewModel.class);
        viewModel.init(groupId, userId);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_group_detail);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_group) {
                openEdit();
                return true;
            } else if (item.getItemId() == R.id.action_delete_group) {
                confirmDelete();
                return true;
            }
            return false;
        });
        setOwnerActionsVisible(false);

        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new TabAdapter(this));
        pager.setOffscreenPageLimit(1);
        TabLayout tabs = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(tabTitle(position)))
                .attach();

        viewModel.group.observe(this, group -> {
            if (group == null) {
                return;
            }
            toolbar.setTitle(group.getName());
            String desc = group.getDescription();
            toolbar.setSubtitle(desc == null || desc.isEmpty() ? null : desc);
            setOwnerActionsVisible(group.isOwner(viewModel.userId()));
        });
        viewModel.loadError.observe(this, message -> {
            if (message != null) {
                showError(message);
            }
        });
        viewModel.error.observe(this, message -> {
            if (message != null) {
                showError(message);
                viewModel.error.setValue(null);
            }
        });
        viewModel.deleted.observe(this, deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                finish();
            }
        });
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private void setOwnerActionsVisible(boolean visible) {
        toolbar.getMenu().findItem(R.id.action_edit_group).setVisible(visible);
        toolbar.getMenu().findItem(R.id.action_delete_group).setVisible(visible);
    }

    private void openEdit() {
        Group group = viewModel.group.getValue();
        if (group == null) {
            return;
        }
        Intent intent = new Intent(this, CreateGroupActivity.class);
        intent.putExtra(GroupListActivity.EXTRA_USER_ID, viewModel.userId());
        intent.putExtra(CreateGroupActivity.EXTRA_GROUP_ID, group.getGroupId());
        intent.putExtra(CreateGroupActivity.EXTRA_NAME, group.getName());
        intent.putExtra(CreateGroupActivity.EXTRA_DESC, group.getDescription());
        editLauncher.launch(intent);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_group_title)
                .setMessage(R.string.confirm_delete_group_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.deleteGroup())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private String tabTitle(int position) {
        switch (position) {
            case 0:
                return getString(R.string.tab_history);
            case 1:
                return getString(R.string.tab_balances);
            case 2:
                return getString(R.string.tab_members);
            default:
                return getString(R.string.tab_summary);
        }
    }

    /** Tab order: History, Balances, Members, Summary. */
    private static class TabAdapter extends FragmentStateAdapter {

        TabAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new HistoryFragment();
                case 1:
                    return new BalanceFragment();
                case 2:
                    return new MemberListFragment();
                default:
                    return new MonthlySummaryFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TAB_COUNT;
        }
    }
}
