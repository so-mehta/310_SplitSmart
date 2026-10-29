package com.example.splitsmart.repository.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.ExecutionException;

/**
 * Turns Firebase's asynchronous Tasks into plain return values so the
 * repositories can match the class diagram's signatures.
 */
final class FirebaseTasks {

    private FirebaseTasks() {
    }

    /**
     * Waits for a Firebase task and returns its result.
     * Must not be called on the main thread (Tasks.await throws IllegalStateException there).
     *
     * @param task the Firebase task to wait for
     * @return the task's result
     * @throws ExecutionException if the task failed; getCause() is the Firebase exception
     */
    static <T> T awaitOrThrow(Task<T> task) throws ExecutionException {
        try {
            return Tasks.await(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Firebase", e);
        }
    }

    /**
     * Waits for a Firebase task and returns its result, turning any failure into
     * a RuntimeException (network errors, permission denied by security rules, etc.).
     *
     * @param task the Firebase task to wait for
     * @return the task's result
     */
    static <T> T await(Task<T> task) {
        try {
            return awaitOrThrow(task);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firebase request failed", e.getCause());
        }
    }
}
