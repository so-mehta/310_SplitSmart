package com.example.splitsmart.repository.firebase;

import com.example.splitsmart.model.Group;
import com.example.splitsmart.repository.GroupRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GroupRepository backed by the Firestore "groups" collection.
 * Each group document is stored at groups/{groupId} with its memberIds as an array,
 * so findByMember is a single array-contains query.
 */
public class FirebaseGroupRepository implements GroupRepository {

    private static final String GROUPS = "groups";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_OWNER_ID = "ownerId";
    private static final String FIELD_MEMBER_IDS = "memberIds";

    private final CollectionReference groups;

    public FirebaseGroupRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseGroupRepository(FirebaseFirestore db) {
        this.groups = db.collection(GROUPS);
    }

    @Override
    public void save(Group group) {
        if (group.getGroupId() == null) {
            group.setGroupId(groups.document().getId());
        }
        FirebaseTasks.await(groups.document(group.getGroupId()).set(toMap(group)));
    }

    /**
     * Replaces the whole group document, including memberIds.
     */
    @Override
    public void update(Group group) {
        FirebaseTasks.await(groups.document(group.getGroupId()).set(toMap(group)));
    }

    /**
     * Deletes the group document only. GroupService deletes the group's
     * expenses and settlements first (cascade).
     */
    @Override
    public void delete(String groupId) {
        FirebaseTasks.await(groups.document(groupId).delete());
    }

    @Override
    public Group findById(String groupId) {
        DocumentSnapshot doc = FirebaseTasks.await(groups.document(groupId).get());
        return doc.exists() ? fromDocument(doc) : null;
    }

    @Override
    public List<Group> findByMember(String userId) {
        QuerySnapshot result = FirebaseTasks.await(
                groups.whereArrayContains(FIELD_MEMBER_IDS, userId).get());
        List<Group> found = new ArrayList<>();
        for (DocumentSnapshot doc : result.getDocuments()) {
            found.add(fromDocument(doc));
        }
        return found;
    }

    private static Map<String, Object> toMap(Group group) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_NAME, group.getName());
        map.put(FIELD_DESCRIPTION, group.getDescription());
        map.put(FIELD_OWNER_ID, group.getOwnerId());
        map.put(FIELD_MEMBER_IDS, new ArrayList<>(group.getMemberIds()));
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Group fromDocument(DocumentSnapshot doc) {
        List<String> memberIds = (List<String>) doc.get(FIELD_MEMBER_IDS);
        return new Group(
                doc.getId(),
                doc.getString(FIELD_NAME),
                doc.getString(FIELD_DESCRIPTION),
                doc.getString(FIELD_OWNER_ID),
                memberIds != null ? new ArrayList<>(memberIds) : new ArrayList<>());
    }
}
