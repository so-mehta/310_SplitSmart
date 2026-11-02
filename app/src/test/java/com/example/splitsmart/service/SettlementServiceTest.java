package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.repository.SettlementRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SettlementServiceTest {

    private static final String GROUP_ID = "group-1";
    private static final String SETTLEMENT_ID = "settlement-1";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CAROL = "carol";
    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2026, 1, 15);

    private BalanceService balanceService;
    private SettlementRepository settlementRepository;
    private SettlementService settlementService;
    private Settlement stored;

    @Before
    public void setUp() {
        balanceService = mock(BalanceService.class);
        settlementRepository = mock(SettlementRepository.class);
        settlementService = new SettlementService(balanceService, settlementRepository);

        stored = new Settlement(SETTLEMENT_ID, GROUP_ID, BOB, 500, ORIGINAL_DATE,
                "First payment", BOB, ALICE);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(stored);
        when(balanceService.getIndividualBalance(GROUP_ID, BOB, ALICE)).thenReturn(-1000L);
    }

    @Test
    public void getOwedMembers_returnsOnlyNegativeBalancesAsPositiveCents() {
        Map<String, Long> balances = new LinkedHashMap<>();
        balances.put(ALICE, -1500L);
        balances.put(CAROL, 700L);
        balances.put("dave", 0L);
        when(balanceService.getBalanceSummary(GROUP_ID, BOB))
                .thenReturn(new BalanceSummary(1500, 700, balances));

        Map<String, Long> owed = settlementService.getOwedMembers(GROUP_ID, BOB);

        assertEquals(1, owed.size());
        assertEquals(Long.valueOf(1500), owed.get(ALICE));
    }

    @Test
    public void createSettlement_acceptsTheExactAmountOwedAndSavesIt() {
        Settlement settlement = newSettlement(1000);

        Settlement created = settlementService.createSettlement(BOB, settlement);

        assertSame(settlement, created);
        assertEquals(BOB, settlement.getCreatorId());
        assertEquals(LocalDate.now(), settlement.getDate());
        verify(settlementRepository).save(settlement);
    }

    @Test
    public void createSettlement_missingPayer_isRejectedWithoutWriting() {
        Settlement settlement = newSettlement(500);
        settlement.setPayerId(null);

        assertThrows(ValidationException.class,
                () -> settlementService.createSettlement(BOB, settlement));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void createSettlement_missingReceiver_isRejectedWithoutWriting() {
        Settlement settlement = newSettlement(500);
        settlement.setReceiverId(null);

        assertThrows(ValidationException.class,
                () -> settlementService.createSettlement(BOB, settlement));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void createSettlement_samePayerAndReceiver_isRejectedWithoutWriting() {
        Settlement settlement = newSettlement(500);
        settlement.setReceiverId(BOB);

        assertThrows(ValidationException.class,
                () -> settlementService.createSettlement(BOB, settlement));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void createSettlement_callerWhoIsNotThePayer_isRejectedWithoutWriting() {
        assertThrows(PermissionException.class,
                () -> settlementService.createSettlement(CAROL, newSettlement(500)));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void createSettlement_nonPositiveAmount_isRejectedWithoutWriting() {
        for (long amount : new long[]{0, -1}) {
            assertThrows(ValidationException.class,
                    () -> settlementService.createSettlement(BOB, newSettlement(amount)));
        }

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void createSettlement_moreThanOwed_isRejectedWithoutWriting() {
        assertThrows(ValidationException.class,
                () -> settlementService.createSettlement(BOB, newSettlement(1001)));

        assertNoWrites();
    }

    @Test
    public void updateSettlement_allowsCurrentDebtPlusTheOldSettlementAmount() {
        Settlement edited = new Settlement(SETTLEMENT_ID, "changed-group", "changed-creator",
                1500, LocalDate.of(2026, 2, 1), "Updated", CAROL, ALICE);

        settlementService.updateSettlement(BOB, edited);

        assertEquals(GROUP_ID, edited.getGroupId());
        assertEquals(BOB, edited.getCreatorId());
        assertEquals(BOB, edited.getPayerId());
        assertEquals(ALICE, edited.getReceiverId());
        assertEquals(ORIGINAL_DATE, edited.getDate());
        verify(settlementRepository).update(edited);
    }

    @Test
    public void updateSettlement_moreThanAdjustedAmountOwed_isRejectedWithoutWriting() {
        Settlement edited = newSettlement(1501);
        edited.setTransactionId(SETTLEMENT_ID);

        assertThrows(ValidationException.class,
                () -> settlementService.updateSettlement(BOB, edited));

        assertNoWrites();
    }

    @Test
    public void updateSettlement_byNonCreator_isRejectedWithoutWriting() {
        Settlement edited = newSettlement(500);
        edited.setTransactionId(SETTLEMENT_ID);

        assertThrows(PermissionException.class,
                () -> settlementService.updateSettlement(CAROL, edited));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void updateSettlement_missingSettlement_isRejectedWithoutWriting() {
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(null);
        Settlement edited = newSettlement(500);
        edited.setTransactionId(SETTLEMENT_ID);

        assertThrows(ValidationException.class,
                () -> settlementService.updateSettlement(BOB, edited));

        assertNoWrites();
        verifyNoInteractions(balanceService);
    }

    @Test
    public void deleteSettlement_byCreator_deletesIt() {
        settlementService.deleteSettlement(BOB, SETTLEMENT_ID);

        verify(settlementRepository).delete(SETTLEMENT_ID);
    }

    @Test
    public void deleteSettlement_byNonCreator_isRejectedWithoutWriting() {
        assertThrows(PermissionException.class,
                () -> settlementService.deleteSettlement(CAROL, SETTLEMENT_ID));

        assertNoWrites();
    }

    @Test
    public void deleteSettlement_missingSettlement_isRejectedWithoutWriting() {
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> settlementService.deleteSettlement(BOB, SETTLEMENT_ID));

        assertNoWrites();
    }

    private static Settlement newSettlement(long amountCents) {
        Settlement settlement = new Settlement(BOB, ALICE);
        settlement.setGroupId(GROUP_ID);
        settlement.setAmountCents(amountCents);
        settlement.setNote("Payment");
        return settlement;
    }

    private void assertNoWrites() {
        verify(settlementRepository, never()).save(any());
        verify(settlementRepository, never()).update(any());
        verify(settlementRepository, never()).delete(any());
    }
}
