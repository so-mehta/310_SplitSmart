package com.example.splitsmart.repository;

import com.example.splitsmart.model.Expense;
import com.example.splitsmart.service.ResultCallback;

import java.util.List;

public interface ExpenseRepository {

    /**
     * Saves a new expense.
     * Calls {@code callback.onSuccess(null)} when saved successfully.
     *
     * @param expense the expense to save
     * @param callback result callback
     */
    void save(Expense expense, ResultCallback<Void> callback);

    /**
     * Updates an existing expense.
     * Calls {@code callback.onSuccess(null)} when updated successfully.
     *
     * @param expense the expense to update
     * @param callback result callback
     */
    void update(Expense expense, ResultCallback<Void> callback);

    /**
     * Deletes an expense by ID.
     * Calls {@code callback.onSuccess(null)} when deleted successfully.
     *
     * @param expenseId the ID of the expense to delete
     * @param callback result callback
     */
    void delete(String expenseId, ResultCallback<Void> callback);

    /**
     * Finds an expense by its ID.
     * Calls {@code callback.onSuccess(Expense)} if found, or {@code callback.onSuccess(null)} if no expense matches.
     *
     * @param expenseId the ID of the expense to find
     * @param callback result callback receiving expense or null if not found
     */
    void findById(String expenseId, ResultCallback<Expense> callback);

    /**
     * Finds all expenses belonging to a specific group.
     * Calls {@code callback.onSuccess(List<Expense>)} containing expenses, or {@code callback.onSuccess(empty list)} if no expenses are found.
     *
     * @param groupId the ID of the group
     * @param callback result callback receiving list of expenses
     */
    void findByGroup(String groupId, ResultCallback<List<Expense>> callback);

    /**
     * Deletes all expenses associated with a group.
     * Calls {@code callback.onSuccess(null)} when deleted successfully.
     *
     * @param groupId the ID of the group whose expenses will be deleted
     * @param callback result callback
     */
    void deleteByGroup(String groupId, ResultCallback<Void> callback);
}
