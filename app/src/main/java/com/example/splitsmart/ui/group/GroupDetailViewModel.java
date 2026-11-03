package com.example.splitsmart.ui.group;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.User;
import com.example.splitsmart.service.GroupService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity-scoped ViewModel shared by GroupDetailActivity and its tab fragments (K12).
 * It loads the group and calls getMembers() once, then every fragment reads from here:
 * <pre>
 * GroupDetailViewModel shared =
 *         new ViewModelProvider(requireActivity()).get(GroupDetailViewModel.class);
 * shared.userId(); shared.groupId(); shared.group; shared.members; shared.memberNames;
 * </pre>
 * To open AddEditExpense, ExpenseDetail or RecordSettlement, call
 * {@link #addContextExtras(Intent)} so the target receives groupId, userId and the member
 * id to name list.
 */
public class GroupDetailViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final GroupService groupService;

    private String groupId;
    private String userId;
    private boolean initialized;

    /** The group, or null while loading or when the user is not a member. */
    public final MutableLiveData<Group> group = new MutableLiveData<>();
    /** The members in member list order. */
    public final MutableLiveData<List<User>> members = new MutableLiveData<>();
    /** Member id to display name, in member list order. */
    public final MutableLiveData<Map<String, String>> memberNames = new MutableLiveData<>();
    /** Set when the group could not be loaded (for example the user is not a member). */
    public final MutableLiveData<String> loadError = new MutableLiveData<>();
    /** For failures of actions such as delete. The Activity shows it with showError. */
    public final MutableLiveData<String> error = new MutableLiveData<>();
    /** Fires true after the group was deleted. */
    public final MutableLiveData<Boolean> deleted = new MutableLiveData<>();
    /** Add-member dialog results. */
    public final MutableLiveData<String> addMemberError = new MutableLiveData<>();
    public final MutableLiveData<Boolean> memberAdded = new MutableLiveData<>();

    public GroupDetailViewModel(@NonNull Application application) {
        super(application);
        groupService = ((SplitSmartApp) application).getGroupService();
    }

    /** Remembers the ids and loads once. Calling it again after a rotation does nothing. */
    public void init(String groupId, String userId) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.groupId = groupId;
        this.userId = userId;
        load();
    }

    public String groupId() {
        return groupId;
    }

    public String userId() {
        return userId;
    }

    /** Reloads the group and its members. */
    public void load() {
        io.execute(() -> {
            try {
                loadBlocking();
            } catch (ValidationException | PermissionException e) {
                loadError.postValue(e.getMessage());
            } catch (RuntimeException e) {
                loadError.postValue(getApplication().getString(R.string.error_generic));
            }
        });
    }

    /** Adds a member by email. The result arrives on memberAdded or addMemberError. */
    public void addMember(String email) {
        io.execute(() -> {
            try {
                groupService.addMemberByEmail(userId, groupId, email);
                loadBlocking();
                memberAdded.postValue(true);
            } catch (ValidationException | PermissionException e) {
                addMemberError.postValue(e.getMessage());
            } catch (RuntimeException e) {
                addMemberError.postValue(getApplication().getString(R.string.error_generic));
            }
        });
    }

    /** Deletes the group. Success arrives on deleted. */
    void deleteGroup() {
        io.execute(() -> {
            try {
                groupService.deleteGroup(userId, groupId);
                deleted.postValue(true);
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(getApplication().getString(R.string.error_generic));
            }
        });
    }

    /**
     * Adds groupId, userId and the member id to name list to an Intent (K12). Targets read them
     * with GroupDetailActivity.EXTRA_GROUP_ID, EXTRA_USER_ID, EXTRA_MEMBER_IDS and
     * EXTRA_MEMBER_NAMES (two lists with the same order).
     */
    public Intent addContextExtras(Intent intent) {
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        Map<String, String> byId = memberNames.getValue();
        if (byId != null) {
            for (Map.Entry<String, String> entry : byId.entrySet()) {
                ids.add(entry.getKey());
                names.add(entry.getValue());
            }
        }
        intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId);
        intent.putExtra(GroupDetailActivity.EXTRA_USER_ID, userId);
        intent.putStringArrayListExtra(GroupDetailActivity.EXTRA_MEMBER_IDS, ids);
        intent.putStringArrayListExtra(GroupDetailActivity.EXTRA_MEMBER_NAMES, names);
        return intent;
    }

    /** Finds the group among the user's groups, then loads its members. */
    private void loadBlocking() {
        Group found = null;
        for (Group candidate : groupService.getGroupsForUser(userId)) {
            if (groupId != null && groupId.equals(candidate.getGroupId())) {
                found = candidate;
                break;
            }
        }
        if (found == null) {
            group.postValue(null);
            members.postValue(Collections.emptyList());
            memberNames.postValue(Collections.emptyMap());
            loadError.postValue(getApplication().getString(R.string.error_not_member));
            return;
        }
        List<User> memberList = groupService.getMembers(groupId);
        Map<String, String> names = new LinkedHashMap<>();
        for (User member : memberList) {
            names.put(member.getUserId(), member.getName());
        }
        group.postValue(found);
        members.postValue(memberList);
        memberNames.postValue(names);
        loadError.postValue(null);
    }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
