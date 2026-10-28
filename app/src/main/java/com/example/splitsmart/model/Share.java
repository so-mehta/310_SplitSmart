package com.example.splitsmart.model;

public class Share {
    private String userId;
    private long shareCents;
    private double percent;

    public Share() {
    }

    public Share(String userId, long shareCents, double percent) {
        this.userId = userId;
        this.shareCents = shareCents;
        this.percent = percent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getShareCents() {
        return shareCents;
    }

    public void setShareCents(long shareCents) {
        this.shareCents = shareCents;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }
}
