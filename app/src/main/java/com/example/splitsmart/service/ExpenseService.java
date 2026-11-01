package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;

import java.util.List;
import java.util.Map;

/**
 * Creating, editing and deleting expenses (SD3, SD4). Everything except previewShares
 * calls the repositories, so never run those methods on the main thread.
 */
public class ExpenseService {

    private final SplitCalculator splitCalculator;
    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    public ExpenseService(SplitCalculator splitCalculator, ExpenseRepository expenseRepository,
                          GroupRepository groupRepository) {
        this.splitCalculator = splitCalculator;
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Saves a new expense, dated today, with the caller as its creator.
     *
     * @param callerId the ID of the user creating the expense
     * @param expense the expense to save
     * @return the saved expense
     * @throws ValidationException if the group does not exist, a field is missing or invalid,
     *                             the payer or a participant is not a member, or the shares do
     *                             not add up to the amount
     * @throws PermissionException if the caller is not a member of the group
     */
    public Expense createExpense(String callerId, Expense expense) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Saves changes to an existing expense.
     *
     * @param callerId the ID of the user making the change
     * @param expense the expense with its new values
     * @throws ValidationException if the expense or group does not exist, a field is missing or
     *                             invalid, the payer or a participant is not a member, or the
     *                             shares do not add up to the amount
     * @throws PermissionException if the caller is not the expense's creator
     */
    public void updateExpense(String callerId, Expense expense) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Deletes an expense.
     *
     * @param callerId the ID of the user making the change
     * @param expenseId the ID of the expense
     * @throws ValidationException if the expense does not exist
     * @throws PermissionException if the caller is not the expense's creator
     */
    public void deleteExpense(String callerId, String expenseId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Gets an expense.
     *
     * @param expenseId the ID of the expense
     * @return the expense
     * @throws ValidationException if the expense does not exist
     */
    public Expense getExpense(String expenseId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Calculates shares for the Add/Edit Expense preview. Makes no repository calls, so it is
     * safe to call on the main thread.
     *
     * @param total the expense total in cents
     * @param method how the total is split
     * @param inputs participant ID to input, in display order: value ignored for EQUAL, cents
     *               for EXACT_AMOUNT, percent for PERCENTAGE
     * @return one share per participant, in the same order
     * @throws ValidationException if there are fewer than 2 participants, or the inputs do not
     *                             add up to the total
     */
    public List<Share> previewShares(long total, SplitMethod method, Map<String, Double> inputs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
