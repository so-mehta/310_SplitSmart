package com.example.splitsmart.repository;

import com.example.splitsmart.model.User;

public interface UserRepository {

    /**
     * Saves a new user record.
     *
     * @param user the user to save
     */
    void save(User user);

    /**
     * Updates an existing user record.
     *
     * @param user the user to update
     */
    void update(User user);

    /**
     * Finds a user by their unique ID.
     *
     * @param userId the ID of the user to find
     * @return the User if found, or null if no user matches
     */
    User findById(String userId);

    /**
     * Finds a user by their email address.
     *
     * @param email the email of the user to find
     * @return the User if found, or null if no user matches
     */
    User findByEmail(String email);

    /**
     * Finds a user by their USC ID.
     *
     * @param uscId the USC ID of the user to find
     * @return the User if found, or null if no user matches
     */
    User findByUscId(String uscId);

    /**
     * Creates a Firebase Auth account. The password is never stored in Firestore.
     *
     * @param email the user's email
     * @param password the user's password
     * @return the new account's user ID
     */
    String createAuthAccount(String email, String password);

    /**
     * Signs in with email and password.
     *
     * @param email the user's email
     * @param password the user's password
     * @return the signed-in user ID, or null if the email or password is wrong
     */
    String signIn(String email, String password);

    /**
     * Signs out the current user.
     */
    void signOut();

    /**
     * Gets the ID of the signed-in user.
     *
     * @return the current user ID, or null if no one is signed in
     */
    String getCurrentUserId();
}
