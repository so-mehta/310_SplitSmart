package com.example.splitsmart.ui.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitsmart.R;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.ui.auth.ProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lists every group the user is in (SD2). A user in zero groups sees an empty state and the
 * Create Group button. The user id arrives as an Intent extra (K12).
 */
public class GroupListActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "com.example.splitsmart.extra.USER_ID";

    private GroupListViewModel viewModel;
    private GroupAdapter adapter;
    private View emptyView;
    private LinearProgressIndicator progress;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_group_list);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra(EXTRA_USER_ID, userId);
                startActivity(intent);
                return true;
            }
            return false;
        });

        emptyView = findViewById(R.id.tv_empty);
        progress = findViewById(R.id.progress);

        adapter = new GroupAdapter(userId, this::openGroup);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_create);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            intent.putExtra(EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        viewModel = new ViewModelProvider(this).get(GroupListViewModel.class);
        viewModel.groups.observe(this, groups -> {
            if (groups == null) {
                return;
            }
            adapter.submit(groups);
            emptyView.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.error.observe(this, message -> {
            if (message != null) {
                showError(message);
                viewModel.error.setValue(null);
            }
        });
        viewModel.loading.observe(this, loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.INVISIBLE));
    }

    /** Reload every time the screen comes back (a group was created, edited or deleted). */
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.load(userId);
    }

    /** Shows a service message (or the generic one) to the user. */
    void showError(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private void openGroup(Group group) {
        Intent intent = new Intent(this, GroupDetailActivity.class);
        intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.getGroupId());
        intent.putExtra(GroupDetailActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    /** One card per group: name, description, member count and an owner note. */
    private static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.Holder> {

        private final String userId;
        private final Consumer<Group> onClick;
        private final List<Group> items = new ArrayList<>();

        GroupAdapter(String userId, Consumer<Group> onClick) {
            this.userId = userId;
            this.onClick = onClick;
        }

        void submit(List<Group> groups) {
            items.clear();
            items.addAll(groups);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Group group = items.get(position);
            int count = group.getMemberIds() == null ? 0 : group.getMemberIds().size();
            String members = holder.itemView.getResources()
                    .getQuantityString(R.plurals.member_count, count, count);
            holder.name.setText(group.getName());
            String desc = group.getDescription();
            holder.desc.setText(desc);
            holder.desc.setVisibility(desc == null || desc.isEmpty() ? View.GONE : View.VISIBLE);
            holder.meta.setText(group.isOwner(userId)
                    ? holder.itemView.getContext().getString(R.string.group_meta_owner, members)
                    : members);
            holder.itemView.setOnClickListener(v -> onClick.accept(group));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView desc;
            final TextView meta;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_name);
                desc = itemView.findViewById(R.id.tv_desc);
                meta = itemView.findViewById(R.id.tv_meta);
            }
        }
    }
}
