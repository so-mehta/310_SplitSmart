package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.repository.SettlementRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Creates, updates and deletes settlements after checking the current amount owed (SD6).
 * Repository-backed methods must not run on the main thread.
 */
public class SettlementService {

    private static final String ERROR_NO_SETTLEMENT = "Settlement does not exist.";
    private static final String ERROR_GROUP = "Choose a group.";
    private static final String ERROR_PAYER = "Choose a payer.";
    private static final String ERROR_RECEIVER = "Choose a receiver.";
    private static final String ERROR_SAME_MEMBER = "Payer and receiver must be different.";
    private static final String ERROR_AMOUNT = "Amount must be greater than 0.";
    private static final String ERROR_NOT_PAYER = "You can only record a settlement you paid.";
    private static final String ERROR_NOT_CREATOR =
            "Only the person who created this settlement can change it.";
    private static final String ERROR_MORE_THAN_OWED =
            "Settlement amount cannot be more than the amount owed.";

    private final BalanceService balanceService;
    private final SettlementRepository settlementRepository;

    public SettlementService(BalanceService balanceService,
                             SettlementRepository settlementRepository) {
        this.balanceService = balanceService;
        this.settlementRepository = settlementRepository;
    }

    /**
     * Creates a settlement.
     *
     * @param callerId the user creating the settlement
     * @param settlement the settlement to create
     * @return the saved settlement
     */
    public Settlement createSettlement(String callerId, Settlement settlement) {
        validateFields(settlement);
        if (!Objects.equals(callerId, settlement.getPayerId())) {
            throw new PermissionException(ERROR_NOT_PAYER);
        }

        checkAmountOwed(settlement.getGroupId(), settlement.getPayerId(),
                settlement.getReceiverId(), settlement.getAmountCents());

        settlement.setCreatorId(callerId);
        settlement.setDate(LocalDate.now());
        settlement.setCreatedAtMillis(System.currentTimeMillis());
        settlementRepository.save(settlement);
        return settlement;
    }

    /**
     * Updates a settlement.
     *
     * @param callerId the user requesting the update
     * @param settlement the settlement with its updated values
     */
    public void updateSettlement(String callerId, Settlement settlement) {
        Settlement stored = findSettlement(
                settlement == null ? null : settlement.getTransactionId());
        checkCreator(callerId, stored);
        if (settlement.getAmountCents() <= 0) {
            throw new ValidationException(ERROR_AMOUNT);
        }

        long currentlyOwed = amountOwed(stored.getGroupId(), stored.getPayerId(),
                stored.getReceiverId());
        long maximumAmount = currentlyOwed + stored.getAmountCents();
        if (settlement.getAmountCents() > maximumAmount) {
            throw new ValidationException(ERROR_MORE_THAN_OWED);
        }

        settlement.setGroupId(stored.getGroupId());
        settlement.setCreatorId(stored.getCreatorId());
        settlement.setDate(stored.getDate());
        settlement.setCreatedAtMillis(stored.getCreatedAtMillis());
        settlement.setPayerId(stored.getPayerId());
        settlement.setReceiverId(stored.getReceiverId());
        settlementRepository.update(settlement);
    }

    /**
     * Deletes a settlement.
     *
     * @param callerId the user requesting the deletion
     * @param settlementId the settlement to delete
     */
    public void deleteSettlement(String callerId, String settlementId) {
        Settlement stored = findSettlement(settlementId);
        checkCreator(callerId, stored);

        settlementRepository.delete(settlementId);
    }

    /**
     * Gets the members the user owes and the amount owed to each one.
     *
     * @param groupId the group whose balances are requested
     * @param userId the user who may owe other members
     * @return member ID to positive amount owed in cents
     */
    public Map<String, Long> getOwedMembers(String groupId, String userId) {
        BalanceSummary summary = balanceService.getBalanceSummary(groupId, userId);
        Map<String, Long> owedMembers = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : summary.getIndividualBalances().entrySet()) {
            if (entry.getValue() < 0) {
                owedMembers.put(entry.getKey(), -entry.getValue());
            }
        }
        return owedMembers;
    }

    private void checkAmountOwed(String groupId, String payerId, String receiverId,
                                 long amountCents) {
        if (amountCents > amountOwed(groupId, payerId, receiverId)) {
            throw new ValidationException(ERROR_MORE_THAN_OWED);
        }
    }

    private long amountOwed(String groupId, String payerId, String receiverId) {
        long balance = balanceService.getIndividualBalance(groupId, payerId, receiverId);
        return balance < 0 ? -balance : 0;
    }

    private Settlement findSettlement(String settlementId) {
        Settlement settlement = isBlank(settlementId)
                ? null : settlementRepository.findById(settlementId);
        if (settlement == null) {
            throw new ValidationException(ERROR_NO_SETTLEMENT);
        }
        return settlement;
    }

    private static void checkCreator(String callerId, Settlement settlement) {
        if (!Objects.equals(callerId, settlement.getCreatorId())) {
            throw new PermissionException(ERROR_NOT_CREATOR);
        }
    }

    private static void validateFields(Settlement settlement) {
        if (settlement == null) {
            throw new ValidationException(ERROR_NO_SETTLEMENT);
        }
        if (isBlank(settlement.getGroupId())) {
            throw new ValidationException(ERROR_GROUP);
        }
        if (isBlank(settlement.getPayerId())) {
            throw new ValidationException(ERROR_PAYER);
        }
        if (isBlank(settlement.getReceiverId())) {
            throw new ValidationException(ERROR_RECEIVER);
        }
        if (settlement.getPayerId().equals(settlement.getReceiverId())) {
            throw new ValidationException(ERROR_SAME_MEMBER);
        }
        if (settlement.getAmountCents() <= 0) {
            throw new ValidationException(ERROR_AMOUNT);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
