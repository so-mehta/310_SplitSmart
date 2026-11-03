package com.example.splitsmart.ui.history;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.splitsmart.exception.*;
import com.example.splitsmart.service.MonthlySummary;
import com.example.splitsmart.service.SummaryService;
import java.time.YearMonth;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MonthlySummaryViewModel extends ViewModel {
    private final SummaryService service;
    private final SavedStateHandle state;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private final Handler main = new Handler(Looper.getMainLooper());
    public final MutableLiveData<MonthlySummary> summary = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private String groupId;

    public MonthlySummaryViewModel(SummaryService service, SavedStateHandle state) { this.service = service; this.state = state; }
    public void initialize(String group) { groupId = group; }
    public YearMonth month() { String value = state.get("month"); return value == null ? YearMonth.now() : YearMonth.parse(value); }
    public void month(YearMonth month) { state.set("month", month.toString()); refresh(); }
    public void refresh() {
        if (groupId == null) return;
        int request = generation.incrementAndGet();
        YearMonth selected = month();
        busy.setValue(true); error.setValue(null); summary.setValue(null);
        io.execute(() -> {
            if (generation.get() != request) return;
            try {
                MonthlySummary result = service.getMonthlySummary(groupId, selected);
                main.post(() -> { if (generation.get() == request) summary.setValue(result); });
            } catch (ValidationException | PermissionException e) {
                main.post(() -> { if (generation.get() == request) error.setValue(e.getMessage()); });
            } catch (RuntimeException e) {
                main.post(() -> { if (generation.get() == request) error.setValue("Something went wrong. Check your connection and try again."); });
            } finally { main.post(() -> { if (generation.get() == request) busy.setValue(false); }); }
        });
    }
    @Override protected void onCleared() { generation.incrementAndGet(); io.shutdownNow(); }
}
