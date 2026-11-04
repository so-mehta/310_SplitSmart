package com.example.splitsmart.ui.history;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.splitsmart.exception.*;
import com.example.splitsmart.model.*;
import com.example.splitsmart.service.HistoryFilter;
import com.example.splitsmart.service.HistoryService;
import com.example.splitsmart.service.SettlementService;
import com.example.splitsmart.ui.common.AmountParser;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HistoryViewModel extends ViewModel {
    private final HistoryService service;
    private final SettlementService settlementService;
    private final SavedStateHandle state;
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ScheduledFuture<?> pending;
    public final MutableLiveData<List<Transaction>> rows = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    /** One-shot result of a settlement edit or delete; the fragment shows it and clears it. */
    public final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private String groupId;

    public HistoryViewModel(HistoryService service, SettlementService settlementService, SavedStateHandle state) {
        this.service = service; this.settlementService = settlementService; this.state = state;
    }
    public void initialize(String group) { groupId = group; }
    public String text(String key) { String value = state.get(key); return value == null ? "" : value; }
    public void set(String key, String value) { state.set(key, value); refresh(); }
    public boolean hasActiveFilters() {
        return !text("keyword").trim().isEmpty() || !text("type").isEmpty() ||
                !text("category").isEmpty() || !text("member").isEmpty();
    }
    public void clearFilters() {
        for (String key : new String[]{"keyword", "type", "category", "member", "order"}) state.set(key, "");
        refresh();
    }

    public void refresh() {
        if (groupId == null) return;
        int request = generation.incrementAndGet();
        if (pending != null) pending.cancel(false);
        HistoryFilter filter = new HistoryFilter(text("keyword"),
                text("type").isEmpty() ? null : TransactionType.valueOf(text("type")),
                text("category").isEmpty() ? null : Category.valueOf(text("category")),
                text("member").isEmpty() ? null : text("member"));
        SortOrder order = text("order").isEmpty() ? SortOrder.NEWEST_FIRST : SortOrder.valueOf(text("order"));
        busy.setValue(true); error.setValue(null);
        pending = io.schedule(() -> {
            try {
                List<Transaction> result = service.getHistory(groupId, filter, order);
                main.post(() -> { if (generation.get() == request) rows.setValue(result); });
            } catch (ValidationException | PermissionException e) {
                main.post(() -> { if (generation.get() == request) { rows.setValue(Collections.emptyList()); error.setValue(e.getMessage()); } });
            } catch (RuntimeException e) {
                main.post(() -> { if (generation.get() == request) { rows.setValue(Collections.emptyList()); error.setValue("Something went wrong. Check your connection and try again."); } });
            } finally { main.post(() -> { if (generation.get() == request) busy.setValue(false); }); }
        }, 250, TimeUnit.MILLISECONDS);
    }

    /** Saves a new amount and note for a settlement the user created, then reloads the history. */
    public void updateSettlement(String callerId, Settlement original, String amountText, String note) {
        io.execute(() -> runAction(() -> {
            Settlement edited = new Settlement(original.getTransactionId(), original.getGroupId(),
                    original.getCreatorId(), AmountParser.parse(amountText), original.getDate(),
                    note == null ? "" : note.trim(), original.getPayerId(), original.getReceiverId());
            settlementService.updateSettlement(callerId, edited);
        }, "Settlement updated."));
    }

    /** Deletes a settlement the user created, then reloads the history. */
    public void deleteSettlement(String callerId, String settlementId) {
        io.execute(() -> runAction(() -> settlementService.deleteSettlement(callerId, settlementId),
                "Settlement deleted."));
    }

    private void runAction(Runnable action, String success) {
        String message;
        boolean changed = false;
        try { action.run(); message = success; changed = true; }
        catch (ValidationException | PermissionException e) { message = e.getMessage(); }
        catch (RuntimeException e) { message = "Something went wrong. Check your connection and try again."; }
        String result = message; boolean reload = changed;
        main.post(() -> { actionMessage.setValue(result); if (reload) refresh(); });
    }

    @Override protected void onCleared() { generation.incrementAndGet(); io.shutdownNow(); }
}
