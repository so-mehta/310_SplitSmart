package com.example.splitsmart.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The separate totals a user owes and is owed, plus their balance with each group member.
 */
public class BalanceSummary {

    private final long totalToPayCents;
    private final long totalToReceiveCents;
    private final Map<String, Long> individualBalances;

    public BalanceSummary(long totalToPayCents, long totalToReceiveCents,
                          Map<String, Long> individualBalances) {
        this.totalToPayCents = totalToPayCents;
        this.totalToReceiveCents = totalToReceiveCents;
        this.individualBalances = immutableCopy(individualBalances);
    }

    public long getTotalToPayCents() {
        return totalToPayCents;
    }

    public long getTotalToReceiveCents() {
        return totalToReceiveCents;
    }

    public Map<String, Long> getIndividualBalances() {
        return individualBalances;
    }

    private static Map<String, Long> immutableCopy(Map<String, Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
