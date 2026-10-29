package com.example.splitsmart.repository;

import com.example.splitsmart.model.Expense;

import java.util.List;

public interface ExpenseRepository {

    /**
     * Saves a new expense.
     *
     * @param expense the expense to save
     */
    void save(Expense expense);

    /**
     * Updates an existing expense.
     *
     * @param expense the expense to update
     */
    void update(Expense expense);

    /**
     * Deletes an expense by ID.
     *
     * @param expenseId the ID of the expense to delete
     */
    void delete(String expenseId);

    /**
     * Finds an expense by its ID.
     *
     * @param expenseId the ID of the expense to find
     * @return the Expense if found, or null if no expense matches
     */
    Expense findById(String expenseId);

    /**
     * Finds all expenses belonging to a specific group.
     *
     * @param groupId the ID of the group
     * @return list of expenses, or empty list if none found
     */
    List<Expense> findByGroup(String groupId);

    /**
     * Deletes all expenses associated with a group.
     *
     * @param groupId the ID of the group whose expenses will be deleted
     */
    void deleteByGroup(String groupId);
}
