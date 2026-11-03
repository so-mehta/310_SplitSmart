package com.example.splitsmart.ui.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.service.GroupService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ViewModel for GroupListActivity (SD2). Service calls run on a background thread (K3). */
public class GroupListViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final GroupService groupService;

    /** Every group the user is in. An empty list means the empty state is shown. */
    final MutableLiveData<List<Group>> groups = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public GroupListViewModel(@NonNull Application application) {
        super(application);
        groupService = ((SplitSmartApp) application).getGroupService();
    }

    void load(String userId) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                groups.postValue(groupService.getGroupsForUser(userId));
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(getApplication().getString(R.string.error_generic));
            } finally {
                loading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
