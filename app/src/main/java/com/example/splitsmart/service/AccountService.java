package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.UserRepository;

/**
 * Sign-up, login and profile changes (SD1). Calls the repository, so never run it
 * on the main thread.
 */
public class AccountService {

    // Firebase Auth rejects anything shorter.
    private static final int MIN_PASSWORD_LENGTH = 6;

    private static final String ERROR_NAME = "Enter your name.";
    private static final String ERROR_EMAIL = "Enter a valid USC email address.";
    private static final String ERROR_USC_ID = "USC ID must be exactly 10 digits.";
    private static final String ERROR_PASSWORD =
            "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
    private static final String ERROR_DUPLICATE_EMAIL = "An account with this email already exists.";
    private static final String ERROR_DUPLICATE_USC_ID = "An account with this USC ID already exists.";
    private static final String ERROR_LOGIN_FIELDS = "Enter your email and password.";
    private static final String ERROR_LOGIN = "Incorrect email or password.";
    private static final String ERROR_NO_PROFILE = "We could not find your profile.";

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
        String trimmedName = trim(name);
        String trimmedEmail = trim(email);
        if (trimmedName.isEmpty()) {
            throw new ValidationException(ERROR_NAME);
        }
        if (!inputValidator.isValidUscEmail(trimmedEmail)) {
            throw new ValidationException(ERROR_EMAIL);
        }
        if (!inputValidator.isValidUscId(uscId)) {
            throw new ValidationException(ERROR_USC_ID);
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(ERROR_PASSWORD);
        }
        if (userRepository.findByEmail(trimmedEmail) != null) {
            throw new ValidationException(ERROR_DUPLICATE_EMAIL);
        }
        if (userRepository.findByUscId(uscId) != null) {
            throw new ValidationException(ERROR_DUPLICATE_USC_ID);
        }

        String userId = userRepository.createAuthAccount(trimmedEmail, password);
        if (userId == null) {
            // Another sign-up took this email after the duplicate check above.
            throw new ValidationException(ERROR_DUPLICATE_EMAIL);
        }
        User user = new User(userId, trimmedName, trimmedEmail, uscId);
        try {
            userRepository.save(user);
        } catch (RuntimeException saveFailure) {
            // Undo the Auth account so the user can sign up again with the same email.
            try {
                userRepository.deleteCurrentAuthAccount();
            } catch (RuntimeException deleteFailure) {
                saveFailure.addSuppressed(deleteFailure);
            }
            throw saveFailure;
        }
        return user;
    }

    /**
     * Logs a user in.
     *
     * @param email the user's email
     * @param password the user's password
     * @return the logged-in user
     * @throws ValidationException if a field is empty, the email or password is wrong, or the
     *                             account has no profile
     */
    public User login(String email, String password) {
        String trimmedEmail = trim(email);
        if (trimmedEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException(ERROR_LOGIN_FIELDS);
        }

        String userId = userRepository.signIn(trimmedEmail, password);
        if (userId == null) {
            throw new ValidationException(ERROR_LOGIN);
        }
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ValidationException(ERROR_NO_PROFILE);
        }
        return user;
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        userRepository.signOut();
    }

    /**
     * Gets the logged-in user.
     *
     * @return the current user, or null if nobody is logged in
     */
    public User getCurrentUser() {
        String userId = userRepository.getCurrentUserId();
        return userId == null ? null : userRepository.findById(userId);
    }

    /**
     * Changes a user's display name.
     *
     * @param userId the ID of the user
     * @param newName the new name
     * @throws ValidationException if the name is empty or the user does not exist
     */
    public void updateName(String userId, String newName) {
        String trimmedName = trim(newName);
        if (trimmedName.isEmpty()) {
            throw new ValidationException(ERROR_NAME);
        }
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ValidationException(ERROR_NO_PROFILE);
        }

        user.setName(trimmedName);
        userRepository.update(user);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
