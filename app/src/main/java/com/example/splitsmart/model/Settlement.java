package com.example.splitsmart.model;

import java.util.Date;

public class Settlement extends Transaction {
    private String payerId;
    private String receiverId;

    public Settlement() {
        super();
    }

    public Settlement(String payerId, String receiverId) {
        super();
        this.payerId = payerId;
        this.receiverId = receiverId;
    }

    public Settlement(String transactionId, String groupId, String creatorId, long amountCents, Date date, String note,
                      String payerId, String receiverId) {
        super(transactionId, groupId, creatorId, amountCents, date, note);
        this.payerId = payerId;
        this.receiverId = receiverId;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.SETTLEMENT;
    }

    @Override
    public boolean involves(String userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(payerId) || userId.equals(receiverId);
    }
}
