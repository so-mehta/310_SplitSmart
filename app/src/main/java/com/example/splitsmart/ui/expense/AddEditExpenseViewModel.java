package com.example.splitsmart.ui.expense;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.*;
import com.example.splitsmart.service.ExpenseService;
import com.example.splitsmart.ui.common.AmountParser;
import com.example.splitsmart.ui.common.MoneyFormatter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Retains the draft across rotation and saved-state recreation. No repository access. */
public class AddEditExpenseViewModel extends ViewModel {
    private final ExpenseService service;
    private final SavedStateHandle state;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<String> previewText = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> valid = new MutableLiveData<>(false);
    private List<Share> preview = Collections.emptyList();
    private LinkedHashMap<String, String> members = new LinkedHashMap<>();
    private String groupId, userId, expenseId;
    private boolean initialized;
    private volatile boolean loading;
    private static final String NETWORK = "Something went wrong. Check your connection and try again.";

    public AddEditExpenseViewModel(ExpenseService service, SavedStateHandle state) {
        this.service = service;
        this.state = state;
    }

    public void initialize(String group, String user, String id, LinkedHashMap<String, String> names) {
        if (initialized) return;
        initialized = true;
        groupId = group; userId = user; expenseId = id; members = new LinkedHashMap<>(names);
        if (id == null || state.contains("draftReady")) {
            if (id == null) {
                if (text("category").isEmpty()) state.set("category", Category.OTHER.name());
                if (text("payer").isEmpty() && !members.isEmpty()) state.set("payer", members.keySet().iterator().next());
                if (!text("payer").isEmpty()) select(text("payer"), true);
            }
            loaded.setValue(true);
            refreshPreview();
        } else load();
    }

    public String text(String key) {
        String value = state.get(key);
        return value == null ? "" : value;
    }

    public void setText(String key, String value) {
        state.set(key, value);
        if ("payer".equals(key) && members.containsKey(value)) select(value, true);
        refreshPreview();
    }

    public ArrayList<String> participants() {
        ArrayList<String> ids = state.get("participants");
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

    public void select(String id, boolean selected) {
        if (!selected && id.equals(text("payer"))) return;
        ArrayList<String> ids = participants();
        if (selected && !ids.contains(id)) ids.add(id);
        if (!selected) ids.remove(id);
        // Display order determines who receives the leftover cent, independent of click order.
        ArrayList<String> ordered = new ArrayList<>();
        for (String member : members.keySet()) if (ids.contains(member)) ordered.add(member);
        state.set("participants", ordered);
        refreshPreview();
    }

    public SplitMethod method() {
        return text("method").isEmpty() ? SplitMethod.EQUAL : SplitMethod.valueOf(text("method"));
    }

    public String input(String id) { return text(method().name() + ":" + id); }
    public void input(String id, String value) { setText(method().name() + ":" + id, value); }

    public void load() {
        if (loading || Boolean.TRUE.equals(busy.getValue())) return;
        loading = true; busy.setValue(true); error.setValue(null);
        io.execute(() -> {
            try {
                Expense expense = service.getExpense(expenseId);
                if (expense == null) throw new ValidationException("Expense does not exist.");
                if (!groupId.equals(expense.getGroupId()) || !userId.equals(expense.getCreatorId()))
                    throw new PermissionException("You cannot edit this expense.");
                // Publish the model; apply it on the main thread through the Activity observer.
                expenseToLoad.postValue(expense);
            } catch (ValidationException | PermissionException e) { error.postValue(e.getMessage()); }
            catch (RuntimeException e) { error.postValue(NETWORK); }
            finally { loading = false; busy.postValue(false); }
        });
    }

    public final MutableLiveData<Expense> expenseToLoad = new MutableLiveData<>();

    public void applyLoadedExpense(Expense e) {
        if (state.contains("draftReady")) return;
        state.set("title", e.getTitle()); state.set("amount", MoneyFormatter.editable(e.getAmountCents()));
        state.set("category", e.getCategory().name()); state.set("payer", e.getPayerId());
        state.set("method", e.getSplitMethod().name()); state.set("note", e.getNote());
        state.set("participants", new ArrayList<>(e.getParticipantIds()));
        state.set("creator", e.getCreatorId()); state.set("date", e.getDate().toString());
        for (Share s : e.getShares()) {
            state.set(SplitMethod.EXACT_AMOUNT.name() + ":" + s.getUserId(), MoneyFormatter.editable(s.getShareCents()));
            state.set(SplitMethod.PERCENTAGE.name() + ":" + s.getUserId(), BigDecimal.valueOf(s.getPercent()).stripTrailingZeros().toPlainString());
        }
        state.set("draftReady", true);
        loaded.setValue(true);
        refreshPreview();
    }

    public void refreshPreview() {
        valid.setValue(false); preview = Collections.emptyList();
        if (!Boolean.TRUE.equals(loaded.getValue())) return;
        try {
            long total = AmountParser.parse(text("amount"));
            LinkedHashMap<String, Double> inputs = new LinkedHashMap<>();
            ArrayList<String> selected = participants();
            if (!selected.contains(text("payer"))) throw new ValidationException("Include the payer in the split.");
            if (selected.size() < 2) throw new ValidationException("Select at least two participants.");
            BigDecimal inputTotal = BigDecimal.ZERO;
            for (String id : members.keySet()) {
                if (!selected.contains(id)) continue;
                double value = 0;
                if (method() == SplitMethod.EXACT_AMOUNT) {
                    long cents = AmountParser.parseNonNegative(input(id));
                    // The agreed service API uses Double inputs. Avoid silently losing integer cents.
                    if (cents > 9007199254740991L) throw new ValidationException("An exact share is too large.");
                    value = cents;
                    inputTotal = inputTotal.add(BigDecimal.valueOf(cents));
                } else if (method() == SplitMethod.PERCENTAGE) {
                    String raw = input(id).trim();
                    if (!raw.matches("[0-9]+(\\.[0-9]{1,2})?"))
                        throw new ValidationException("Enter percentages with at most two decimal places.");
                    BigDecimal pct = new BigDecimal(raw);
                    if (pct.compareTo(BigDecimal.valueOf(100)) > 0)
                        throw new ValidationException("Each percentage must be between 0 and 100.");
                    value = pct.doubleValue();
                    inputTotal = inputTotal.add(pct);
                }
                inputs.put(id, value);
            }
            if (inputs.size() != selected.size()) throw new ValidationException("A participant is no longer a group member.");
            if (method() == SplitMethod.EXACT_AMOUNT) {
                BigDecimal difference = BigDecimal.valueOf(total).subtract(inputTotal);
                if (difference.signum() != 0) throw new ValidationException("Exact shares are $" +
                        difference.abs().movePointLeft(2).setScale(2).toPlainString() +
                        (difference.signum() > 0 ? " short. Increase the shares to match the total." : " over. Decrease the shares to match the total."));
            }
            if (method() == SplitMethod.PERCENTAGE && inputTotal.compareTo(BigDecimal.valueOf(100)) != 0)
                throw new ValidationException("Percentages total " + inputTotal.stripTrailingZeros().toPlainString() + "%. Adjust them to total 100%.");
            List<Share> shares = service.previewShares(total, method(), inputs);
            long sum = 0;
            Set<String> seen = new HashSet<>();
            StringBuilder display = new StringBuilder();
            for (Share s : shares) {
                if (s.getShareCents() < 0 || !inputs.containsKey(s.getUserId()) || !seen.add(s.getUserId()))
                    throw new ValidationException("Invalid split preview.");
                sum = Math.addExact(sum, s.getShareCents());
                display.append(members.get(s.getUserId())).append(": ").append(MoneyFormatter.format(s.getShareCents())).append("\n");
            }
            if (sum != total || seen.size() != inputs.size()) throw new ValidationException("Shares must add up to the total.");
            preview = new ArrayList<>(shares);
            previewText.setValue(display.toString().trim());
            if (text("title").trim().isEmpty()) previewText.setValue(display.toString().trim() + "\nEnter an expense title to save.");
            valid.setValue(!text("title").trim().isEmpty() && members.containsKey(text("payer")) &&
                    !text("category").isEmpty() && groupId != null && userId != null);
        } catch (ValidationException e) { previewText.setValue(e.getMessage()); }
        catch (ArithmeticException e) { previewText.setValue("Split amount is too large."); }
        catch (RuntimeException e) { previewText.setValue("Unable to calculate this split. Please try again."); }
    }

    public void save() {
        if (!Boolean.TRUE.equals(loaded.getValue()) || Boolean.TRUE.equals(busy.getValue()) ||
                Boolean.TRUE.equals(saved.getValue())) return;
        refreshPreview();
        if (!Boolean.TRUE.equals(valid.getValue())) return;
        Expense expense = new Expense();
        expense.setTransactionId(expenseId); expense.setGroupId(groupId);
        expense.setCreatorId(expenseId == null ? userId : text("creator"));
        if (!text("date").isEmpty()) expense.setDate(java.time.LocalDate.parse(text("date")));
        expense.setTitle(text("title").trim()); expense.setAmountCents(AmountParser.parse(text("amount")));
        expense.setCategory(Category.valueOf(text("category"))); expense.setPayerId(text("payer"));
        expense.setNote(text("note").trim()); expense.setSplitMethod(method());
        expense.setShares(new ArrayList<>(preview));
        busy.setValue(true); error.setValue(null);
        io.execute(() -> {
            try {
                if (expenseId == null) service.createExpense(userId, expense);
                else service.updateExpense(userId, expense);
                saved.postValue(true);
            } catch (ValidationException | PermissionException e) { error.postValue(e.getMessage()); }
            catch (RuntimeException e) { error.postValue(NETWORK); }
            finally { busy.postValue(false); }
        });
    }

    @Override protected void onCleared() { io.shutdownNow(); }
}
