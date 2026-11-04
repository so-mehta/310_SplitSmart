package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creating, editing and deleting expenses (SD3, SD4). Everything except previewShares
 * calls the repositories, so never run those methods on the main thread.
 */
public class ExpenseService {

    private static final int MIN_PARTICIPANTS = 2;

    // TODO(Q2): replace with exact Project 2.1 text
    private static final String ERROR_NO_GROUP = "This group no longer exists.";
    private static final String ERROR_NO_EXPENSE = "Expense does not exist.";
    private static final String ERROR_NOT_MEMBER = "You are not a member of this group.";
    private static final String ERROR_NOT_CREATOR = "Only the person who created this expense can change it.";
    private static final String ERROR_TITLE = "Enter an expense title.";
    private static final String ERROR_AMOUNT = "Amount must be greater than 0.";
    private static final String ERROR_PAYER = "The payer must be a member of this group.";
    private static final String ERROR_PARTICIPANTS = "Select at least two participants.";
    private static final String ERROR_DUPLICATE_PARTICIPANT = "Each participant can only have one share.";
    private static final String ERROR_PARTICIPANT = "Every participant must be a member of this group.";
    private static final String ERROR_PAYER_NOT_PARTICIPANT = "The payer must be one of the participants.";
    private static final String ERROR_CATEGORY = "Choose a category.";
    private static final String ERROR_METHOD = "Choose a split method.";
    private static final String ERROR_SHARES = "Shares must add up to the total.";

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
     *                             the payer or a participant is not a member, there are fewer
     *                             than 2 participants, or the shares do not add up to the amount
     * @throws PermissionException if the caller is not a member of the group
     */
    public Expense createExpense(String callerId, Expense expense) {
        Group group = findGroup(expense.getGroupId());
        checkMember(callerId, group);
        checkFields(expense, group);

        expense.setCreatorId(callerId);
        expense.setDate(LocalDate.now());
        expense.setCreatedAtMillis(System.currentTimeMillis());
        expenseRepository.save(expense);
        return expense;
    }

    /**
     * Saves changes to an existing expense. The creator, group and date cannot change, so the
     * stored values replace any on the expense passed in.
     *
     * @param callerId the ID of the user making the change
     * @param expense the expense with its new values
     * @throws ValidationException if the expense or group does not exist, a field is missing or
     *                             invalid, the payer or a participant is not a member, there are
     *                             fewer than 2 participants, or the shares do not add up to the
     *                             amount
     * @throws PermissionException if the caller is not the expense's creator or not a member of
     *                             the group
     */
    public void updateExpense(String callerId, Expense expense) {
        Expense stored = findExpense(expense.getTransactionId());
        checkCreator(callerId, stored);
        Group group = findGroup(stored.getGroupId());
        checkMember(callerId, group);
        checkFields(expense, group);

        expense.setCreatorId(stored.getCreatorId());
        expense.setGroupId(stored.getGroupId());
        expense.setDate(stored.getDate());
        expense.setCreatedAtMillis(stored.getCreatedAtMillis());
        expenseRepository.update(expense);
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
        checkCreator(callerId, findExpense(expenseId));

        expenseRepository.delete(expenseId);
    }

    /**
     * Gets an expense.
     *
     * @param expenseId the ID of the expense
     * @return the expense
     * @throws ValidationException if the expense does not exist
     */
    public Expense getExpense(String expenseId) {
        return findExpense(expenseId);
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
        if (inputs == null) {
            throw new ValidationException(ERROR_PARTICIPANTS);
        }
        return splitCalculator.calculate(total, method, new ArrayList<>(inputs.keySet()), inputs);
    }

    private Group findGroup(String groupId) {
        Group group = groupId == null ? null : groupRepository.findById(groupId);
        if (group == null) {
            throw new ValidationException(ERROR_NO_GROUP);
        }
        return group;
    }

    private Expense findExpense(String expenseId) {
        Expense expense = expenseId == null ? null : expenseRepository.findById(expenseId);
        if (expense == null) {
            throw new ValidationException(ERROR_NO_EXPENSE);
        }
        return expense;
    }

    private static void checkMember(String callerId, Group group) {
        if (!group.isMember(callerId)) {
            throw new PermissionException(ERROR_NOT_MEMBER);
        }
    }

    private static void checkCreator(String callerId, Expense stored) {
        if (callerId == null || !callerId.equals(stored.getCreatorId())) {
            throw new PermissionException(ERROR_NOT_CREATOR);
        }
    }

    private void checkFields(Expense expense, Group group) {
        if (expense.getTitle() == null || expense.getTitle().trim().isEmpty()) {
            throw new ValidationException(ERROR_TITLE);
        }
        if (expense.getAmountCents() <= 0) {
            throw new ValidationException(ERROR_AMOUNT);
        }
        if (!group.isMember(expense.getPayerId())) {
            throw new ValidationException(ERROR_PAYER);
        }
        checkParticipants(expense, group);
        if (expense.getCategory() == null) {
            throw new ValidationException(ERROR_CATEGORY);
        }
        if (expense.getSplitMethod() == null) {
            throw new ValidationException(ERROR_METHOD);
        }
        if (!splitCalculator.validateShares(expense.getAmountCents(), expense.getShares())) {
            throw new ValidationException(ERROR_SHARES);
        }
    }

    private static void checkParticipants(Expense expense, Group group) {
        List<String> participantIds = expense.getParticipantIds();
        if (participantIds.size() < MIN_PARTICIPANTS) {
            throw new ValidationException(ERROR_PARTICIPANTS);
        }
        // getParticipantIds skips nulls and repeats, so a size mismatch finds both.
        if (participantIds.size() != expense.getShares().size()) {
            throw new ValidationException(ERROR_DUPLICATE_PARTICIPANT);
        }
        for (String participantId : participantIds) {
            if (!group.isMember(participantId)) {
                throw new ValidationException(ERROR_PARTICIPANT);
            }
        }
        if (!participantIds.contains(expense.getPayerId())) {
            throw new ValidationException(ERROR_PAYER_NOT_PARTICIPANT);
        }
    }
}
