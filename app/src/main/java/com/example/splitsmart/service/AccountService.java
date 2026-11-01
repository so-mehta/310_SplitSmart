package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.UserRepository;

/**
 * Sign-up, login and profile changes (SD1). Calls the repository, so never run it
 * on the main thread.
 */
public class AccountService {

    private final UserRepository userRepository;
    private final InputValidator inputValidator;

    public AccountService(UserRepository userRepository, InputValidator inputValidator) {
        this.userRepository = userRepository;
        this.inputValidator = inputValidator;
    }

    /**
     * Creates an account and its user profile.
     *
     * @param name the user's name
     * @param email the user's USC email
     * @param uscId the user's 10-digit USC ID
     * @param password the user's password
     * @return the new user
     * @throws ValidationException if a field is empty or invalid, or the email or USC ID is
     *                             already registered
     */
    public User register(String name, String email, String uscId, String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Logs a user in.
     *
     * @param email the user's email
     * @param password the user's password
     * @return the logged-in user
     * @throws ValidationException if a field is empty, or the email or password is wrong
     */
    public User login(String email, String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Gets the logged-in user.
     *
     * @return the current user, or null if nobody is logged in
     */
    public User getCurrentUser() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Changes a user's display name.
     *
     * @param userId the ID of the user
     * @param newName the new name
     * @throws ValidationException if the name is empty or the user does not exist
     */
    public void updateName(String userId, String newName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
