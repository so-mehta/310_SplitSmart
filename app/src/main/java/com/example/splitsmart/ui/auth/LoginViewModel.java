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

/**
 * ViewModel for LoginActivity (SD1). Service calls run on a background thread, never the
 * main thread (K3).
 */
public class LoginViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AccountService accountService;

    /** Set when somebody is logged in (after login or when one was already logged in). */
    final MutableLiveData<User> user = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) {
        super(application);
        accountService = ((SplitSmartApp) application).getAccountService();
    }

    /** Skips the login screen when a user is already logged in (K12). */
    void checkCurrentUser() {
        loading.setValue(true);
        io.execute(() -> {
            try {
                User current = accountService.getCurrentUser();
                if (current != null) {
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

    void login(String email, String password) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                user.postValue(accountService.login(email, password));
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
