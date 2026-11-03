package com.example.splitsmart.ui.expense;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.service.ExpenseService;
import java.util.concurrent.*;

public class ExpenseDetailViewModel extends ViewModel {
    private final ExpenseService service;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    public final MutableLiveData<Expense> expense = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private volatile boolean running;

    public ExpenseDetailViewModel(ExpenseService service) { this.service = service; }

    public void load(String groupId, String id) {
        if (running) return;
        running = true; busy.setValue(true); error.setValue(null);
        io.execute(() -> {
            try {
                Expense e = service.getExpense(id);
                if (e == null) throw new ValidationException("Expense does not exist.");
                if (!groupId.equals(e.getGroupId())) throw new PermissionException("This expense belongs to another group.");
                expense.postValue(e);
            } catch (ValidationException | PermissionException e) { expense.postValue(null); error.postValue(e.getMessage()); }
            catch (RuntimeException e) { error.postValue("Something went wrong. Check your connection and try again."); }
            finally { running = false; busy.postValue(false); }
        });
    }

    public void delete(String userId) {
        Expense e = expense.getValue();
        if (running || e == null || !userId.equals(e.getCreatorId()) || Boolean.TRUE.equals(deleted.getValue())) return;
        running = true; busy.setValue(true); error.setValue(null);
        io.execute(() -> {
            try { service.deleteExpense(userId, e.getTransactionId()); deleted.postValue(true); }
            catch (ValidationException | PermissionException ex) { error.postValue(ex.getMessage()); }
            catch (RuntimeException ex) { error.postValue("Something went wrong. Check your connection and try again."); }
            finally { running = false; busy.postValue(false); }
        });
    }

    @Override protected void onCleared() { io.shutdownNow(); }
}
