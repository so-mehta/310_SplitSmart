package com.example.splitsmart.ui.balance;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.service.SettlementService;
import com.example.splitsmart.ui.common.AmountParser;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for RecordSettlementActivity (SD6). Service calls run on a background thread (K3).
 * A ValidationException (from AmountParser or the service) keeps the user on the screen and
 * saves nothing.
 */
public class RecordSettlementViewModel extends AndroidViewModel {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final SettlementService settlementService;

    /** Member id to the cents the user owes that member (positive). Empty means nobody. */
    final MutableLiveData<Map<String, Long>> owed = new MutableLiveData<>();
    /** Fires true after the settlement was saved. */
    final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public RecordSettlementViewModel(@NonNull Application application) {
        super(application);
        settlementService = ((SplitSmartApp) application).getSettlementService();
    }

    void loadOwed(String groupId, String userId) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                owed.postValue(settlementService.getOwedMembers(groupId, userId));
            } catch (ValidationException | PermissionException e) {
                error.postValue(e.getMessage());
            } catch (RuntimeException e) {
                error.postValue(getApplication().getString(R.string.error_generic));
            } finally {
                loading.postValue(false);
            }
        });
    }

    /** The caller is always the payer: you can only record a settlement you paid. */
    void save(String groupId, String userId, String receiverId, String amountText, String note) {
        loading.setValue(true);
        io.execute(() -> {
            try {
                long amountCents = AmountParser.parse(amountText);
                Settlement settlement = new Settlement(userId, receiverId);
                settlement.setGroupId(groupId);
                settlement.setAmountCents(amountCents);
                settlement.setNote(note == null ? "" : note.trim());
                settlementService.createSettlement(userId, settlement);
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
