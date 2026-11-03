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

/** ViewModel for SignUpActivity (SD1). Service calls run on a background thread (K3). */
public class SignUpViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AccountService accountService;

    final MutableLiveData<User> user = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public SignUpViewModel(@NonNull Application application) {
        super(application);
        accountService = ((SplitSmartApp) application).getAccountService();
    }

    void register(String name, String email, String uscId, String password) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                user.postValue(accountService.register(name, email, uscId, password));
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
