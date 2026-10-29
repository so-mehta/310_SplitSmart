package com.example.splitsmart.repository.firebase;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExpenseRepository backed by the top-level Firestore "expenses" collection.
 * Each document has a groupId field, so one query loads a whole group's expenses.
 * Dates are stored as YYYY-MM-DD strings, money as long cents, enums by name,
 * and shares as a list of maps.
 */
public class FirebaseExpenseRepository implements ExpenseRepository {

    private static final String EXPENSES = "expenses";
    private static final String FIELD_GROUP_ID = "groupId";
    private static final String FIELD_CREATOR_ID = "creatorId";
    private static final String FIELD_AMOUNT_CENTS = "amountCents";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_NOTE = "note";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_PAYER_ID = "payerId";
    private static final String FIELD_SPLIT_METHOD = "splitMethod";
    private static final String FIELD_SHARES = "shares";
    private static final String SHARE_USER_ID = "userId";
    private static final String SHARE_CENTS = "shareCents";
    private static final String SHARE_PERCENT = "percent";

    private final FirebaseFirestore db;
    private final CollectionReference expenses;

    public FirebaseExpenseRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseExpenseRepository(FirebaseFirestore db) {
        this.db = db;
        this.expenses = db.collection(EXPENSES);
    }

    @Override
    public void save(Expense expense) {
        if (expense.getTransactionId() == null) {
            expense.setTransactionId(expenses.document().getId());
        }
        FirebaseTasks.await(expenses.document(expense.getTransactionId()).set(toMap(expense)));
    }

    @Override
    public void update(Expense expense) {
        FirebaseTasks.await(expenses.document(expense.getTransactionId()).set(toMap(expense)));
    }

    @Override
    public void delete(String expenseId) {
        FirebaseTasks.await(expenses.document(expenseId).delete());
    }

    @Override
    public Expense findById(String expenseId) {
        DocumentSnapshot doc = FirebaseTasks.await(expenses.document(expenseId).get());
        return doc.exists() ? fromDocument(doc) : null;
    }

    @Override
    public List<Expense> findByGroup(String groupId) {
        QuerySnapshot result = FirebaseTasks.await(
                expenses.whereEqualTo(FIELD_GROUP_ID, groupId).get());
        List<Expense> found = new ArrayList<>();
        for (DocumentSnapshot doc : result.getDocuments()) {
            found.add(fromDocument(doc));
        }
        return found;
    }

    /**
     * Deletes every expense in the group, in batches of at most 500 writes (Firestore's limit).
     */
    @Override
    public void deleteByGroup(String groupId) {
        QuerySnapshot result = FirebaseTasks.await(
                expenses.whereEqualTo(FIELD_GROUP_ID, groupId).get());
        FirestoreBatches.deleteAll(db, result.getDocuments());
    }

    private static Map<String, Object> toMap(Expense expense) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_GROUP_ID, expense.getGroupId());
        map.put(FIELD_CREATOR_ID, expense.getCreatorId());
        map.put(FIELD_AMOUNT_CENTS, expense.getAmountCents());
        map.put(FIELD_DATE, expense.getDate() != null ? expense.getDate().toString() : null);
        map.put(FIELD_NOTE, expense.getNote());
        map.put(FIELD_TITLE, expense.getTitle());
        map.put(FIELD_CATEGORY, expense.getCategory() != null ? expense.getCategory().name() : null);
        map.put(FIELD_PAYER_ID, expense.getPayerId());
        map.put(FIELD_SPLIT_METHOD, expense.getSplitMethod() != null ? expense.getSplitMethod().name() : null);
        List<Map<String, Object>> shares = new ArrayList<>();
        for (Share share : expense.getShares()) {
            Map<String, Object> shareMap = new HashMap<>();
            shareMap.put(SHARE_USER_ID, share.getUserId());
            shareMap.put(SHARE_CENTS, share.getShareCents());
            shareMap.put(SHARE_PERCENT, share.getPercent());
            shares.add(shareMap);
        }
        map.put(FIELD_SHARES, shares);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Expense fromDocument(DocumentSnapshot doc) {
        List<Share> shares = new ArrayList<>();
        List<Map<String, Object>> storedShares = (List<Map<String, Object>>) doc.get(FIELD_SHARES);
        if (storedShares != null) {
            for (Map<String, Object> shareMap : storedShares) {
                shares.add(new Share(
                        (String) shareMap.get(SHARE_USER_ID),
                        toLong(shareMap.get(SHARE_CENTS)),
                        toDouble(shareMap.get(SHARE_PERCENT))));
            }
        }
        String date = doc.getString(FIELD_DATE);
        String category = doc.getString(FIELD_CATEGORY);
        String splitMethod = doc.getString(FIELD_SPLIT_METHOD);
        return new Expense(
                doc.getId(),
                doc.getString(FIELD_GROUP_ID),
                doc.getString(FIELD_CREATOR_ID),
                toLong(doc.get(FIELD_AMOUNT_CENTS)),
                date != null ? LocalDate.parse(date) : null,
                doc.getString(FIELD_NOTE),
                doc.getString(FIELD_TITLE),
                category != null ? Category.valueOf(category) : null,
                doc.getString(FIELD_PAYER_ID),
                splitMethod != null ? SplitMethod.valueOf(splitMethod) : null,
                shares);
    }

    // Firestore returns numbers as Long or Double depending on how they were written.
    private static long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }
}
