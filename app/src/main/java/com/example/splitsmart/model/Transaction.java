package com.example.splitsmart.model;

import java.util.Date;

public abstract class Transaction {
    private String transactionId;
    private String groupId;
    private String creatorId;
    private long amountCents;
    private Date date;
    private String note;

    public Transaction() {
    }

    public Transaction(String transactionId, String groupId, String creatorId, long amountCents, Date date, String note) {
        this.transactionId = transactionId;
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.amountCents = amountCents;
        this.date = date;
        this.note = note;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public abstract TransactionType getType();

    public abstract boolean involves(String userId);
}
