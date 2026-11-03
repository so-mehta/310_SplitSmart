package com.example.splitsmart.ui.balance;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.service.BalanceService;
import com.example.splitsmart.service.BalanceSummary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ViewModel for BalanceFragment (SD5). Service calls run on a background thread (K3). */
public class BalanceViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final BalanceService balanceService;

    final MutableLiveData<BalanceSummary> summary = new MutableLiveData<>();
    /** The failure message, or null after a successful load. */
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public BalanceViewModel(@NonNull Application application) {
        super(application);
        balanceService = ((SplitSmartApp) application).getBalanceService();
    }

    void load(String groupId, String userId) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                summary.postValue(balanceService.getBalanceSummary(groupId, userId));
                error.postValue(null);
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
