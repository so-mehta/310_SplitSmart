package com.example.splitsmart;

import android.app.Application;

import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;
import com.example.splitsmart.repository.SettlementRepository;
import com.example.splitsmart.repository.UserRepository;
import com.example.splitsmart.repository.firebase.FirebaseExpenseRepository;
import com.example.splitsmart.repository.firebase.FirebaseGroupRepository;
import com.example.splitsmart.repository.firebase.FirebaseSettlementRepository;
import com.example.splitsmart.repository.firebase.FirebaseUserRepository;
import com.example.splitsmart.service.AccountService;
import com.example.splitsmart.service.BalanceService;
import com.example.splitsmart.service.ExpenseService;
import com.example.splitsmart.service.GroupService;
import com.example.splitsmart.service.HistoryService;
import com.example.splitsmart.service.InputValidator;
import com.example.splitsmart.service.SettlementService;
import com.example.splitsmart.service.SplitCalculator;
import com.example.splitsmart.service.SummaryService;

/**
 * This is the only class allowed to import both the repository implementations
 * and the services
 */
public class SplitSmartApp extends Application {

    private AccountService accountService;
    private GroupService groupService;
    private ExpenseService expenseService;
    private BalanceService balanceService;
    private SettlementService settlementService;
    private HistoryService historyService;
    private SummaryService summaryService;

    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase is initialized automatically by the google-services plugin.
        UserRepository userRepository = new FirebaseUserRepository();
        GroupRepository groupRepository = new FirebaseGroupRepository();
        ExpenseRepository expenseRepository = new FirebaseExpenseRepository();
        SettlementRepository settlementRepository = new FirebaseSettlementRepository();

        accountService = new AccountService(userRepository, new InputValidator());
        groupService = new GroupService(groupRepository, userRepository, expenseRepository,
                settlementRepository);
        expenseService = new ExpenseService(new SplitCalculator(), expenseRepository, groupRepository);
        balanceService = new BalanceService(expenseRepository, settlementRepository);
        settlementService = new SettlementService(balanceService, settlementRepository);
        historyService = new HistoryService(expenseRepository, settlementRepository);
        summaryService = new SummaryService(expenseRepository);
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public ExpenseService getExpenseService() {
        return expenseService;
    }

    public BalanceService getBalanceService() {
        return balanceService;
    }

    public SettlementService getSettlementService() {
        return settlementService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public SummaryService getSummaryService() {
        return summaryService;
    }
}
