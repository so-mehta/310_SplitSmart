package com.example.splitsmart.ui.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.service.GroupService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for CreateGroupActivity (SD2). The same screen also edits a group, which is why
 * it has both createGroup and updateGroup. Service calls run on a background thread (K3).
 */
public class CreateGroupViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final GroupService groupService;

    /** Fires true after the group was created or updated. */
    final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public CreateGroupViewModel(@NonNull Application application) {
        super(application);
        groupService = ((SplitSmartApp) application).getGroupService();
    }

    void createGroup(String ownerId, String name, String desc) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                groupService.createGroup(ownerId, name, desc);
                saved.postValue(true);
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(getApplication().getString(R.string.error_generic));
            } finally {
                loading.postValue(false);
            }
        });
    }

    void updateGroup(String callerId, String groupId, String name, String desc) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                groupService.updateGroup(callerId, groupId, name, desc);
                saved.postValue(true);
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
