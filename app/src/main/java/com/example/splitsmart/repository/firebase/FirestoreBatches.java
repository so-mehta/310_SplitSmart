package com.example.splitsmart.repository.firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

/**
 * Batch deletes for deleteByGroup. Firestore allows at most 500 writes per batch,
 * and a group can have more transactions than that (NFR 5.2.4 tests 500).
 */
final class FirestoreBatches {

    static final int MAX_WRITES_PER_BATCH = 500;

    private FirestoreBatches() {
    }

    /**
     * Deletes all the given documents, committing a new batch every 500 deletes.
     *
     * @param db the Firestore instance
     * @param docs the documents to delete
     */
    static void deleteAll(FirebaseFirestore db, List<DocumentSnapshot> docs) {
        for (int start = 0; start < docs.size(); start += MAX_WRITES_PER_BATCH) {
            int end = Math.min(start + MAX_WRITES_PER_BATCH, docs.size());
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : docs.subList(start, end)) {
                batch.delete(doc.getReference());
            }
            FirebaseTasks.await(batch.commit());
        }
    }
}
