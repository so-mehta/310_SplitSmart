package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * White-box tests for ExpenseService (Feature 2). ExpenseServiceTest checks that bad input is
 * rejected, but not which rule fires first or what message the user sees. These tests follow the
 * order of the guards in createExpense, updateExpense, deleteExpense and findExpense, using
 * hand-written fakes so each test can count exactly which repository calls were made.
 */
public class SimraExpenseServiceTest {

    private static final String GROUP_ID = "group-1";
    private static final String EXPENSE_ID = "expense-1";
    private static final String SIMRA = "simra";
    private static final String CHRISTINE = "christine";
    private static final String ALEX = "alex";
    private static final String OUTSIDER = "outsider";

    private FakeExpenseRepository expenses;
    private FakeGroupRepository groups;
    private ExpenseService service;

    @Before
    public void setUp() {
        expenses = new FakeExpenseRepository();
        groups = new FakeGroupRepository();
        groups.group = new Group(GROUP_ID, "Apartment", "", SIMRA,
                new ArrayList<>(Arrays.asList(SIMRA, CHRISTINE, ALEX)));
        service = new ExpenseService(new SplitCalculator(), expenses, groups);
    }

    @Test
    public void wb1_createExpense_looksUpTheGroupThenChecksMembershipThenTheFields() {
        Expense invalid = validExpense();
        invalid.setTitle("  ");

        // Group lookup comes first, so a missing group wins over a non-member and a bad title.
        groups.group = null;
        assertValidation("This group no longer exists.", () -> service.createExpense(OUTSIDER, invalid));

        // With the group back, membership is checked before any field, for an outsider and for null.
        groups.group = new Group(GROUP_ID, "Apartment", "", SIMRA,
                new ArrayList<>(Arrays.asList(SIMRA, CHRISTINE, ALEX)));
        assertPermission("You are not a member of this group.", () -> service.createExpense(OUTSIDER, invalid));
        assertPermission("You are not a member of this group.", () -> service.createExpense(null, invalid));

        // A member reaches the field checks.
        assertValidation("Enter an expense title.", () -> service.createExpense(SIMRA, invalid));

        assertEquals(0, expenses.writes);
    }

    @Test
    public void wb2_createExpense_fieldRulesRunInOrderAndEachHasItsOwnMessage() {
        Expense e = validExpense();
        e.setTitle("  ");
        e.setAmountCents(0);
        e.setPayerId(OUTSIDER);
        e.setShares(shares(SIMRA, 1000));
        e.setCategory(null);
        e.setSplitMethod(null);

        assertValidation("Enter an expense title.", () -> service.createExpense(SIMRA, e));
        e.setTitle("Dinner");
        assertValidation("Amount must be greater than zero.", () -> service.createExpense(SIMRA, e));
        e.setAmountCents(1000);
        assertValidation("The payer must be a member of this group.", () -> service.createExpense(SIMRA, e));
        e.setPayerId(SIMRA);
        assertValidation("Select at least two participants.", () -> service.createExpense(SIMRA, e));
        e.setShares(shares(SIMRA, 500, SIMRA, 250, CHRISTINE, 250));
        assertValidation("Each participant can only have one share.", () -> service.createExpense(SIMRA, e));
        e.setShares(shares(SIMRA, 500, OUTSIDER, 500));
        assertValidation("Every participant must be a member of this group.",
                () -> service.createExpense(SIMRA, e));
        e.setShares(shares(CHRISTINE, 500, ALEX, 500));
        assertValidation("The payer must be one of the participants.", () -> service.createExpense(SIMRA, e));
        e.setShares(shares(SIMRA, 500, CHRISTINE, 499));
        assertValidation("Choose a category.", () -> service.createExpense(SIMRA, e));
        e.setCategory(Category.FOOD);
        assertValidation("Choose a split method.", () -> service.createExpense(SIMRA, e));
        e.setSplitMethod(SplitMethod.EQUAL);
        assertValidation("Shares must add up to the total.", () -> service.createExpense(SIMRA, e));

        assertEquals(0, expenses.writes);

        e.setShares(shares(SIMRA, 500, CHRISTINE, 500));
        service.createExpense(SIMRA, e);
        assertEquals(1, expenses.writes);
    }

    @Test
    public void wb3_updateAndDelete_nullCallerOrWrongCallerIsRefusedBeforeTheGroupIsTouched() {
        expenses.stored = storedExpense(CHRISTINE);

        // checkCreator() has its own null branch, so a null caller is refused, not a crash.
        String notCreator = "Only the person who created this expense can change it.";
        assertPermission(notCreator, () -> service.deleteExpense(null, EXPENSE_ID));
        assertPermission(notCreator, () -> service.updateExpense(null, validExpense(EXPENSE_ID)));

        // A stored expense with no creator must not be matched by a null caller.
        expenses.stored = storedExpense(null);
        assertPermission(notCreator, () -> service.deleteExpense(null, EXPENSE_ID));

        // The creator check runs before the group lookup: with the group gone, a non-creator
        // gets the permission message and the real creator gets the group message.
        expenses.stored = storedExpense(CHRISTINE);
        groups.group = null;
        assertPermission(notCreator, () -> service.updateExpense(ALEX, validExpense(EXPENSE_ID)));
        assertValidation("This group no longer exists.",
                () -> service.updateExpense(CHRISTINE, validExpense(EXPENSE_ID)));

        assertEquals(0, expenses.writes);
    }

    @Test
    public void wb4_deleteAndGetExpense_nullIdIsMissingWithoutALookupButBlankIdIsLookedUp() {
        assertValidation("Expense does not exist.", () -> service.deleteExpense(CHRISTINE, null));
        assertValidation("Expense does not exist.", () -> service.getExpense(null));
        assertEquals(0, expenses.findCalls);

        // Only null is short-circuited: an empty ID still goes to the repository.
        assertValidation("Expense does not exist.", () -> service.getExpense(""));
        assertEquals(1, expenses.findCalls);
        assertEquals(0, expenses.writes);
    }

    @Test
    public void wb5_createExpense_failureOnTheLastCheckLeavesTheExpenseUntouched() {
        Expense e = validExpense();
        e.setShares(shares(SIMRA, 500, CHRISTINE, 499));
        long createdBefore = e.getCreatedAtMillis();

        assertValidation("Shares must add up to the total.", () -> service.createExpense(SIMRA, e));

        // creator, date and time are only set after every check has passed.
        assertNull(e.getCreatorId());
        assertNull(e.getDate());
        assertEquals(createdBefore, e.getCreatedAtMillis());
        assertEquals(0, expenses.writes);
    }

    private static Expense validExpense() {
        Expense expense = new Expense("Dinner", Category.FOOD, SIMRA, SplitMethod.EQUAL,
                shares(SIMRA, 500, CHRISTINE, 500));
        expense.setGroupId(GROUP_ID);
        expense.setAmountCents(1000);
        return expense;
    }

    private static Expense validExpense(String id) {
        Expense expense = validExpense();
        expense.setTransactionId(id);
        return expense;
    }

    private static Expense storedExpense(String creatorId) {
        Expense expense = validExpense(EXPENSE_ID);
        expense.setCreatorId(creatorId);
        expense.setDate(LocalDate.of(2026, 1, 15));
        return expense;
    }

    private static List<Share> shares(Object... idsAndCents) {
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < idsAndCents.length; i += 2) {
            shares.add(new Share((String) idsAndCents[i], (Integer) idsAndCents[i + 1], 0));
        }
        return shares;
    }

    private static void assertValidation(String message, Runnable action) {
        assertEquals(message, assertThrows(ValidationException.class, action::run).getMessage());
    }

    private static void assertPermission(String message, Runnable action) {
        assertEquals(message, assertThrows(PermissionException.class, action::run).getMessage());
    }

    private static class FakeExpenseRepository implements ExpenseRepository {
        Expense stored;
        int findCalls;
        int writes;

        @Override
        public void save(Expense expense) {
            writes++;
        }

        @Override
        public void update(Expense expense) {
            writes++;
        }

        @Override
        public void delete(String expenseId) {
            writes++;
        }

        @Override
        public Expense findById(String expenseId) {
            findCalls++;
            return stored;
        }

        @Override
        public List<Expense> findByGroup(String groupId) {
            return new ArrayList<>();
        }

        @Override
        public void deleteByGroup(String groupId) {
            writes++;
        }
    }

    private static class FakeGroupRepository implements GroupRepository {
        Group group;

        @Override
        public void save(Group group) {
        }

        @Override
        public void update(Group group) {
        }

        @Override
        public void delete(String groupId) {
        }

        @Override
        public Group findById(String groupId) {
            return group;
        }

        @Override
        public List<Group> findByMember(String userId) {
            return new ArrayList<>();
        }
    }
}
