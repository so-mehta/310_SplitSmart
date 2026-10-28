package com.example.splitsmart.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Expense extends Transaction {
    private String title;
    private Category category;
    private String payerId;
    private SplitMethod splitMethod;
    private List<Share> shares;

    public Expense() {
        super();
        this.shares = new ArrayList<>();
    }

    public Expense(String title, Category category, String payerId, SplitMethod splitMethod, List<Share> shares) {
        super();
        this.title = title;
        this.category = category;
        this.payerId = payerId;
        this.splitMethod = splitMethod;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    public Expense(String transactionId, String groupId, String creatorId, long amountCents, Date date, String note,
                   String title, Category category, String payerId, SplitMethod splitMethod, List<Share> shares) {
        super(transactionId, groupId, creatorId, amountCents, date, note);
        this.title = title;
        this.category = category;
        this.payerId = payerId;
        this.splitMethod = splitMethod;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public SplitMethod getSplitMethod() {
        return splitMethod;
    }

    public void setSplitMethod(SplitMethod splitMethod) {
        this.splitMethod = splitMethod;
    }

    public List<Share> getShares() {
        return shares;
    }

    public void setShares(List<Share> shares) {
        this.shares = shares;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.EXPENSE;
    }

    @Override
    public boolean involves(String userId) {
        if (userId == null) {
            return false;
        }
        if (userId.equals(payerId)) {
            return true;
        }
        if (shares != null) {
            for (Share share : shares) {
                if (share != null && userId.equals(share.getUserId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getParticipantIds() {
        Set<String> participantIds = new LinkedHashSet<>();
        if (payerId != null) {
            participantIds.add(payerId);
        }
        if (shares != null) {
            for (Share share : shares) {
                if (share != null && share.getUserId() != null) {
                    participantIds.add(share.getUserId());
                }
            }
        }
        return new ArrayList<>(participantIds);
    }

    public Share getShareOf(String userId) {
        if (shares == null || userId == null) {
            return null;
        }
        for (Share share : shares) {
            if (share != null && userId.equals(share.getUserId())) {
                return share;
            }
        }
        return null;
    }
}
