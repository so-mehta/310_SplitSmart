package com.example.splitsmart.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.User;
import com.example.splitsmart.service.AccountService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ViewModel for ProfileActivity (SD1). Service calls run on a background thread (K3). */
public class ProfileViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AccountService accountService;

    final MutableLiveData<User> user = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** Fires true after the name was saved. */
    final MutableLiveData<Boolean> nameSaved = new MutableLiveData<>();
    /** Fires true after logout. */
    final MutableLiveData<Boolean> loggedOut = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        accountService = ((SplitSmartApp) application).getAccountService();
    }

    void load() {
        loading.setValue(true);
        io.execute(() -> {
            try {
                User current = accountService.getCurrentUser();
                if (current == null) {
                    // The session ended; send the user back to the login screen.
                    loggedOut.postValue(true);
                } else {
                    user.postValue(current);
                }
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(genericError());
            } finally {
                loading.postValue(false);
            }
        });
    }

    void saveName(String userId, String newName) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                accountService.updateName(userId, newName);
                user.postValue(accountService.getCurrentUser());
                nameSaved.postValue(true);
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(genericError());
            } finally {
                loading.postValue(false);
            }
        });
    }

    void logout() {
        loading.setValue(true);
        io.execute(() -> {
            try {
                accountService.logout();
                loggedOut.postValue(true);
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(genericError());
            } finally {
                loading.postValue(false);
            }
        });
    }

    private String genericError() {
        return getApplication().getString(R.string.error_generic);
    }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
