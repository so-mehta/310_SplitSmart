package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.TransactionType;

/**
 * Optional filters for a group's combined expense and settlement history.
 */
public class HistoryFilter {

    private final String keyword;
    private final TransactionType type;
    private final Category category;
    private final String memberId;

    public HistoryFilter(String keyword, TransactionType type, Category category, String memberId) {
        this.keyword = keyword;
        this.type = type;
        this.category = category;
        this.memberId = memberId;
    }

    public String getKeyword() {
        return keyword;
    }

    public TransactionType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public String getMemberId() {
        return memberId;
    }
}
