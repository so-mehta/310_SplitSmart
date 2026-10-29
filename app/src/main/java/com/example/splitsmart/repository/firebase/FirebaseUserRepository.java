package com.example.splitsmart.repository.firebase;

import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * UserRepository backed by Firebase Authentication and the Firestore "users" collection.
 * Each user document is stored at users/{userId}, where userId is the Firebase Auth uid.
 * Passwords only ever go to Firebase Auth, never to Firestore.
 */
public class FirebaseUserRepository implements UserRepository {

    private static final String USERS = "users";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_USC_ID = "uscId";

    private final FirebaseAuth auth;
    private final CollectionReference users;

    public FirebaseUserRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    public FirebaseUserRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.auth = auth;
        this.users = db.collection(USERS);
    }

    @Override
    public void save(User user) {
        if (user.getUserId() == null) {
            user.setUserId(users.document().getId());
        }
        FirebaseTasks.await(users.document(user.getUserId()).set(toMap(user)));
    }

    @Override
    public void update(User user) {
        FirebaseTasks.await(users.document(user.getUserId()).set(toMap(user)));
    }

    @Override
    public User findById(String userId) {
        DocumentSnapshot doc = FirebaseTasks.await(users.document(userId).get());
        return doc.exists() ? fromDocument(doc) : null;
    }

    @Override
    public User findByEmail(String email) {
        return findFirst(FIELD_EMAIL, normalizeEmail(email));
    }

    @Override
    public User findByUscId(String uscId) {
        return findFirst(FIELD_USC_ID, uscId);
    }

    @Override
    public String createAuthAccount(String email, String password) {
        return FirebaseTasks.await(auth.createUserWithEmailAndPassword(normalizeEmail(email), password))
                .getUser()
                .getUid();
    }

    @Override
    public String signIn(String email, String password) {
        try {
            return FirebaseTasks.awaitOrThrow(auth.signInWithEmailAndPassword(normalizeEmail(email), password))
                    .getUser()
                    .getUid();
        } catch (ExecutionException e) {
            // Wrong password or no such account: report as "not signed in" so the
            // service can throw a ValidationException with the Project 2.1 message.
            if (e.getCause() instanceof FirebaseAuthInvalidCredentialsException
                    || e.getCause() instanceof FirebaseAuthInvalidUserException) {
                return null;
            }
            throw new RuntimeException("Firebase sign-in failed", e.getCause());
        }
    }

    @Override
    public void signOut() {
        auth.signOut();
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser current = auth.getCurrentUser();
        return current != null ? current.getUid() : null;
    }

    private User findFirst(String field, String value) {
        QuerySnapshot result = FirebaseTasks.await(users.whereEqualTo(field, value).limit(1).get());
        return result.isEmpty() ? null : fromDocument(result.getDocuments().get(0));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_NAME, user.getName());
        map.put(FIELD_EMAIL, normalizeEmail(user.getEmail()));
        map.put(FIELD_USC_ID, user.getUscId());
        return map;
    }

    private static User fromDocument(DocumentSnapshot doc) {
        return new User(
                doc.getId(),
                doc.getString(FIELD_NAME),
                doc.getString(FIELD_EMAIL),
                doc.getString(FIELD_USC_ID));
    }
}
