package com.example.splitsmart.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitsmart.R;
import com.example.splitsmart.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Members tab (SD2). Marks the owner and shows Add Member to the owner only, in a dialog that
 * shows the ValidationException or PermissionException message and stays open on failure.
 * Reads the shared {@link GroupDetailViewModel} (K12).
 */
public class MemberListFragment extends Fragment {

    private GroupDetailViewModel shared;
    private MemberAdapter adapter;
    private Button addButton;

    // The open add-member dialog, if any.
    private AlertDialog addDialog;
    private TextInputLayout addEmailLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_member_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shared = new ViewModelProvider(requireActivity()).get(GroupDetailViewModel.class);

        adapter = new MemberAdapter();
        RecyclerView recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        addButton = view.findViewById(R.id.btn_add_member);
        addButton.setOnClickListener(v -> showAddMemberDialog());

        shared.group.observe(getViewLifecycleOwner(), group -> {
            adapter.setOwnerId(group == null ? null : group.getOwnerId());
            addButton.setVisibility(group != null && group.isOwner(shared.userId())
                    ? View.VISIBLE : View.GONE);
        });
        shared.members.observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                adapter.submit(members);
            }
        });
        shared.addMemberError.observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            shared.addMemberError.setValue(null);
            if (addDialog != null) {
                addEmailLayout.setError(message);
                addDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });
        shared.memberAdded.observe(getViewLifecycleOwner(), added -> {
            if (!Boolean.TRUE.equals(added)) {
                return;
            }
            shared.memberAdded.setValue(null);
            if (addDialog != null) {
                addDialog.dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (addDialog != null) {
            addDialog.dismiss();
        }
        super.onDestroyView();
    }

    private void showAddMemberDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_add_member, null);
        addEmailLayout = content.findViewById(R.id.til_email);
        TextInputEditText emailInput = content.findViewById(R.id.et_email);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_add_member)
                .setView(content)
                .setPositiveButton(R.string.action_add, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        // Set the click listener after show() so a failure keeps the dialog open.
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                addEmailLayout.setError(null);
                positive.setEnabled(false);
                String email = emailInput.getText() == null
                        ? "" : emailInput.getText().toString();
                shared.addMember(email);
            });
        });
        dialog.setOnDismissListener(d -> {
            addDialog = null;
            addEmailLayout = null;
        });
        addDialog = dialog;
        dialog.show();
    }

    /** One row per member: name, email and an Owner badge. */
    private static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.Holder> {

        private final List<User> items = new ArrayList<>();
        private String ownerId;

        void submit(List<User> members) {
            items.clear();
            items.addAll(members);
            notifyDataSetChanged();
        }

        void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            User member = items.get(position);
            holder.name.setText(member.getName());
            holder.email.setText(member.getEmail());
            boolean isOwner = ownerId != null && ownerId.equals(member.getUserId());
            holder.ownerBadge.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView email;
            final TextView ownerBadge;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_name);
                email = itemView.findViewById(R.id.tv_email);
                ownerBadge = itemView.findViewById(R.id.tv_owner_badge);
            }
        }
    }
}
