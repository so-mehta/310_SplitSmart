package com.example.splitsmart.repository;

import com.example.splitsmart.model.User;
import com.example.splitsmart.service.ResultCallback;

public interface UserRepository {

    /**
     * Saves a new user record.
     * Calls {@code callback.onSuccess(null)} when saved successfully.
     *
     * @param user the user to save
     * @param callback result callback
     */
    void save(User user, ResultCallback<Void> callback);

    /**
     * Updates an existing user record.
     * Calls {@code callback.onSuccess(null)} when updated successfully.
     *
     * @param user the user to update
     * @param callback result callback
     */
    void update(User user, ResultCallback<Void> callback);

    /**
     * Finds a user by their unique ID.
     * Calls {@code callback.onSuccess(User)} if found, or {@code callback.onSuccess(null)} if no user matches.
     *
     * @param userId the ID of the user to find
     * @param callback result callback receiving the user or null if not found
     */
    void findById(String userId, ResultCallback<User> callback);

    /**
     * Finds a user by their email address.
     * Calls {@code callback.onSuccess(User)} if found, or {@code callback.onSuccess(null)} if no user matches.
     *
     * @param email the email of the user to find
     * @param callback result callback receiving the user or null if not found
     */
    void findByEmail(String email, ResultCallback<User> callback);

    /**
     * Finds a user by their USC ID.
     * Calls {@code callback.onSuccess(User)} if found, or {@code callback.onSuccess(null)} if no user matches.
     *
     * @param uscId the USC ID of the user to find
     * @param callback result callback receiving the user or null if not found
     */
    void findByUscId(String uscId, ResultCallback<User> callback);
}
