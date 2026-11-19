package com.example.splitsmart;

import com.example.splitsmart.model.*;
import com.example.splitsmart.repository.*;
import com.example.splitsmart.service.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Test APK only. Real history/summary services with isolated, editable in-memory repository fixtures. */
public class SomyaHistoryTestApplication extends SplitSmartApp {
    public static final String GROUP = "somya-history-fixture";
    public static final String USER = "somya";
    public YearMonth baseMonth;
    private List<Expense> expenses;
    private List<Settlement> settlements;
    private GroupService groups;
    private HistoryService history;
    private SummaryService summary;
    private BalanceService balances;
    private ExpenseService expenseService;
    private SettlementService settlementService;

    @Override public void onCreate() {
        // Deliberately do not construct the production Firebase repositories.
        resetFixtures();
    }

    public void resetFixtures() {
        baseMonth = YearMonth.now();
        expenses = new java.util.concurrent.CopyOnWriteArrayList<>(Arrays.asList(
                expense("stay", "Stay", 9000, baseMonth.minusMonths(1).atEndOfMonth(), Category.HOUSING, "alex", "somya", "alex"),
                expense("pizza", "Pizza", 3000, baseMonth.atDay(1), Category.FOOD, "somya", "somya", "alex"),
                expense("lunch", "Lunch", 1000, baseMonth.atDay(15), Category.FOOD, "christine", "somya", "christine"),
                expense("rent", "Rent", 120000, baseMonth.atEndOfMonth(), Category.HOUSING, "somya", "somya", "alex"),
                expense("uber", "Uber", 2500, baseMonth.plusMonths(1).atDay(1), Category.TRANSPORTATION, "alex", "somya", "alex")));
        expenses.get(3).setNote("lease renewal");
        Settlement payment = new Settlement("payment", GROUP, "alex", 700,
                baseMonth.atDay(15), "pizza refund", "alex", "somya");
        payment.setCreatedAtMillis(300);
        expenses.get(2).setCreatedAtMillis(200);
        settlements = new java.util.concurrent.CopyOnWriteArrayList<>(Collections.singletonList(payment));
        ExpenseRepository expenseRepo = new ExpenseRepository() {
            public List<Expense> findByGroup(String id) { return GROUP.equals(id) ? new ArrayList<>(expenses) : Collections.emptyList(); }
            public Expense findById(String id) { for (Expense e : expenses) if (e.getTransactionId().equals(id)) return e; return null; }
            public void save(Expense e) { e.setTransactionId(UUID.randomUUID().toString()); expenses.add(e); }
            public void update(Expense e) { delete(e.getTransactionId()); expenses.add(e); }
            public void delete(String id) { expenses.removeIf(e -> id.equals(e.getTransactionId())); }
            public void deleteByGroup(String id) { expenses.removeIf(e -> id.equals(e.getGroupId())); }
        };
        SettlementRepository settlementRepo = new SettlementRepository() {
            public List<Settlement> findByGroup(String id) { return GROUP.equals(id) ? new ArrayList<>(settlements) : Collections.emptyList(); }
            public Settlement findById(String id) { for (Settlement s : settlements) if (s.getTransactionId().equals(id)) return s; return null; }
            public void save(Settlement s) { s.setTransactionId(UUID.randomUUID().toString()); settlements.add(s); }
            public void update(Settlement s) { delete(s.getTransactionId()); settlements.add(s); }
            public void delete(String id) { settlements.removeIf(s -> id.equals(s.getTransactionId())); }
            public void deleteByGroup(String id) { settlements.removeIf(s -> id.equals(s.getGroupId())); }
        };
        Group group = new Group(GROUP, "Somya History QA", "Isolated UI test data", USER,
                Arrays.asList("somya", "alex", "christine"));
        List<User> members = Arrays.asList(new User("somya", "Somya", "somya@usc.edu", "1000000001"),
                new User("alex", "Alex", "alex@usc.edu", "1000000002"),
                new User("christine", "Christine", "christine@usc.edu", "1000000003"));
        groups = new GroupService(null, null, expenseRepo, settlementRepo) {
            @Override public List<Group> getGroupsForUser(String id) {
                return group.isMember(id) ? Collections.singletonList(group) : Collections.emptyList();
            }
            @Override public List<User> getMembers(String id) {
                return GROUP.equals(id) ? members : Collections.emptyList();
            }
        };
        GroupRepository groupRepo = new GroupRepository() {
            public Group findById(String id) { return GROUP.equals(id) ? group : null; }
            public List<Group> findByMember(String id) { return group.isMember(id) ? Collections.singletonList(group) : Collections.emptyList(); }
            public void save(Group g) { throw new UnsupportedOperationException(); }
            public void update(Group g) { throw new UnsupportedOperationException(); }
            public void delete(String id) { throw new UnsupportedOperationException(); }
        };
        expenseService = new ExpenseService(new SplitCalculator(), expenseRepo, groupRepo);
        history = new HistoryService(expenseRepo, settlementRepo);
        summary = new SummaryService(expenseRepo);
        balances = new BalanceService(expenseRepo, settlementRepo);
        settlementService = new SettlementService(balances, settlementRepo);
    }

    @Override public GroupService getGroupService() { return groups; }
    @Override public HistoryService getHistoryService() { return history; }
    @Override public SummaryService getSummaryService() { return summary; }
    @Override public BalanceService getBalanceService() { return balances; }

    @Override public ExpenseService getExpenseService() { return expenseService; }
    @Override public SettlementService getSettlementService() { return settlementService; }
    private static Expense expense(String id, String title, long amount, LocalDate date,
                                   Category category, String payer, String... participants) {
        List<Share> shares = new ArrayList<>();
        for (String user : participants) shares.add(new Share(user, amount / participants.length, 0));
        return new Expense(id, GROUP, USER, amount, date, "", title, category, payer, SplitMethod.EQUAL, shares);
    }
}
