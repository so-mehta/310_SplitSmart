package com.example.splitsmart.repository.firebase;

import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.repository.SettlementRepository;
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
 * SettlementRepository backed by the top-level Firestore "settlements" collection.
 * Each document has a groupId field, so one query loads a whole group's settlements.
 * Dates are stored as YYYY-MM-DD strings and money as long cents.
 */
public class FirebaseSettlementRepository implements SettlementRepository {

    private static final String SETTLEMENTS = "settlements";
    private static final String FIELD_GROUP_ID = "groupId";
    private static final String FIELD_CREATOR_ID = "creatorId";
    private static final String FIELD_AMOUNT_CENTS = "amountCents";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_NOTE = "note";
    private static final String FIELD_PAYER_ID = "payerId";
    private static final String FIELD_RECEIVER_ID = "receiverId";

    private final FirebaseFirestore db;
    private final CollectionReference settlements;

    public FirebaseSettlementRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseSettlementRepository(FirebaseFirestore db) {
        this.db = db;
        this.settlements = db.collection(SETTLEMENTS);
    }

    @Override
    public void save(Settlement settlement) {
        if (settlement.getTransactionId() == null) {
            settlement.setTransactionId(settlements.document().getId());
        }
        FirebaseTasks.await(settlements.document(settlement.getTransactionId()).set(toMap(settlement)));
    }

    @Override
    public void update(Settlement settlement) {
        FirebaseTasks.await(settlements.document(settlement.getTransactionId()).set(toMap(settlement)));
    }

    @Override
    public void delete(String settlementId) {
        FirebaseTasks.await(settlements.document(settlementId).delete());
    }

    @Override
    public Settlement findById(String settlementId) {
        DocumentSnapshot doc = FirebaseTasks.await(settlements.document(settlementId).get());
        return doc.exists() ? fromDocument(doc) : null;
    }

    @Override
    public List<Settlement> findByGroup(String groupId) {
        QuerySnapshot result = FirebaseTasks.await(
                settlements.whereEqualTo(FIELD_GROUP_ID, groupId).get());
        List<Settlement> found = new ArrayList<>();
        for (DocumentSnapshot doc : result.getDocuments()) {
            found.add(fromDocument(doc));
        }
        return found;
    }

    /**
     * Deletes every settlement in the group, in batches of at most 500 writes (Firestore's limit).
     */
    @Override
    public void deleteByGroup(String groupId) {
        QuerySnapshot result = FirebaseTasks.await(
                settlements.whereEqualTo(FIELD_GROUP_ID, groupId).get());
        FirestoreBatches.deleteAll(db, result.getDocuments());
    }

    private static Map<String, Object> toMap(Settlement settlement) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_GROUP_ID, settlement.getGroupId());
        map.put(FIELD_CREATOR_ID, settlement.getCreatorId());
        map.put(FIELD_AMOUNT_CENTS, settlement.getAmountCents());
        map.put(FIELD_DATE, settlement.getDate() != null ? settlement.getDate().toString() : null);
        map.put(FIELD_NOTE, settlement.getNote());
        map.put(FIELD_PAYER_ID, settlement.getPayerId());
        map.put(FIELD_RECEIVER_ID, settlement.getReceiverId());
        return map;
    }

    private static Settlement fromDocument(DocumentSnapshot doc) {
        String date = doc.getString(FIELD_DATE);
        Long amountCents = doc.getLong(FIELD_AMOUNT_CENTS);
        return new Settlement(
                doc.getId(),
                doc.getString(FIELD_GROUP_ID),
                doc.getString(FIELD_CREATOR_ID),
                amountCents != null ? amountCents : 0L,
                date != null ? LocalDate.parse(date) : null,
                doc.getString(FIELD_NOTE),
                doc.getString(FIELD_PAYER_ID),
                doc.getString(FIELD_RECEIVER_ID));
    }
}
