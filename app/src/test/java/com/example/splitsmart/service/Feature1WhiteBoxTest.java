package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;
import com.example.splitsmart.repository.SettlementRepository;
import com.example.splitsmart.repository.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Feature 1 (User and Group Management) white-box tests.
 *
 * These tests are written from the source of AccountService, GroupService, InputValidator and
 * Group. Each one targets specific branches or paths (guard order, short-circuits, early exits,
 * null checks) and uses Mockito to confirm which repository calls each path makes.
 */
public class Feature1WhiteBoxTest {

    private static final String OWNER = "owner";
    private static final String MEMBER = "member";
    private static final String GROUP_ID = "group-1";

    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private AccountService accountService;
    private GroupService groupService;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        groupRepository = mock(GroupRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        accountService = new AccountService(userRepository, new InputValidator());
        groupService = new GroupService(groupRepository, userRepository, expenseRepository,
                settlementRepository);
    }

    /**
     * WB1 - Path coverage of the guard chain in AccountService.register(). The guards run in a
     * fixed order (name -> email -> USC ID -> password). Starting with every field invalid and
     * fixing one field at a time drives execution through each guard's true branch in turn, so
     * every error path is checked with its exact message. No path may reach the repository.
     */
    @Test
    public void wb1_register_guardChainFiresInOrderWithExactMessages() {
        assertRegisterMessage("Enter your name.", "  ", "bad@gmail.com", "12345abcde", "123");
        assertRegisterMessage("Enter a valid USC email address.",
                "Tommy", "bad@gmail.com", "12345abcde", "123");
        assertRegisterMessage("USC ID must be exactly 10 digits.",
                "Tommy", "tommy@usc.edu", "12345abcde", "123");
        assertRegisterMessage("Password must be at least 6 characters.",
                "Tommy", "tommy@usc.edu", "1234567890", "123");

        verifyNoInteractions(userRepository);
    }

    /**
     * WB2 - Branch coverage of the duplicate checks in register(). When both the email and the
     * USC ID are already taken, the duplicate-email branch is taken first and the method exits
     * before findByUscId() is ever called. When only the ID is taken, the email branch is false
     * and the duplicate-ID branch is true. Neither path may create an Auth account.
     */
    @Test
    public void wb2_register_duplicateEmailShortCircuitsBeforeUscIdLookup() {
        when(userRepository.findByEmail("tommy@usc.edu")).thenReturn(new User());
        when(userRepository.findByUscId("1234567890")).thenReturn(new User());

        ValidationException both = assertThrows(ValidationException.class,
                () -> accountService.register("Tommy", "tommy@usc.edu", "1234567890", "fighton"));
        assertEquals("An account with this email already exists.", both.getMessage());
        verify(userRepository).findByEmail("tommy@usc.edu");
        verify(userRepository, never()).findByUscId(any());
        verify(userRepository, never()).createAuthAccount(any(), any());

        reset(userRepository);
        when(userRepository.findByUscId("1234567890")).thenReturn(new User());

        ValidationException idOnly = assertThrows(ValidationException.class,
                () -> accountService.register("Tommy", "new@usc.edu", "1234567890", "fighton"));
        assertEquals("An account with this USC ID already exists.", idOnly.getMessage());
        InOrder order = inOrder(userRepository);
        order.verify(userRepository).findByEmail("new@usc.edu");
        order.verify(userRepository).findByUscId("1234567890");
        verify(userRepository, never()).createAuthAccount(any(), any());
    }

    /**
     * WB3 - Statement coverage of updateName(). The only field the method changes is name, via
     * setName(). The User passed to update() must keep the stored email and USC ID unchanged,
     * which is how the code enforces "email and USC ID cannot be edited after registration".
     */
    @Test
    public void wb3_updateName_onlyMutatesNameAndPersistsOriginalEmailAndUscId() {
        User stored = new User("uid-1", "Tommy", "tommy@usc.edu", "1234567890");
        when(userRepository.findById("uid-1")).thenReturn(stored);

        accountService.updateName("uid-1", "  Traveler  ");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).findById("uid-1");
        verify(userRepository).update(saved.capture());
        verifyNoMoreInteractions(userRepository);
        assertEquals("uid-1", saved.getValue().getUserId());
        assertEquals("Traveler", saved.getValue().getName());
        assertEquals("tommy@usc.edu", saved.getValue().getEmail());
        assertEquals("1234567890", saved.getValue().getUscId());
    }

    /**
     * WB4 - Branch coverage of addMemberByEmail(): user == null (true/false) and
     * group.isMember(userId) (true/false). Checks the exact explanatory message for an
     * unregistered email and for an existing member, and that only the fully-false path
     * reaches groupRepository.update().
     */
    @Test
    public void wb4_addMemberByEmail_coversUnknownEmailAlreadyMemberAndSuccessBranches() {
        Group group = new Group(GROUP_ID, "Trip", "", OWNER,
                new ArrayList<>(Arrays.asList(OWNER, MEMBER)));
        when(groupRepository.findById(GROUP_ID)).thenReturn(group);
        when(userRepository.findByEmail("ghost@usc.edu")).thenReturn(null);
        when(userRepository.findByEmail("member@usc.edu"))
                .thenReturn(new User(MEMBER, "Member", "member@usc.edu", "1000000002"));
        when(userRepository.findByEmail("new@usc.edu"))
                .thenReturn(new User("new", "New", "new@usc.edu", "1000000003"));

        ValidationException unknown = assertThrows(ValidationException.class,
                () -> groupService.addMemberByEmail(OWNER, GROUP_ID, "ghost@usc.edu"));
        assertEquals("No account was found with this email.", unknown.getMessage());

        ValidationException already = assertThrows(ValidationException.class,
                () -> groupService.addMemberByEmail(OWNER, GROUP_ID, "member@usc.edu"));
        assertEquals("This user is already a member of the group.", already.getMessage());

        verify(groupRepository, never()).update(any());

        groupService.addMemberByEmail(OWNER, GROUP_ID, "new@usc.edu");
        verify(groupRepository, times(1)).update(group);
        assertEquals(Arrays.asList(OWNER, MEMBER, "new"), group.getMemberIds());

        // The null branches of Group.isMember() ("memberIds != null && ...") and of the
        // constructor's "memberIds != null ? memberIds : new ArrayList<>()".
        Group fromNullList = new Group("g", "n", "", OWNER, null);
        assertEquals(0, fromNullList.getMemberIds().size());
        fromNullList.setMemberIds(null);
        assertFalse(fromNullList.isMember(OWNER));
    }

    /**
     * WB5 - Condition coverage of Group.isOwner() inside findGroupOwnedBy(), on the deleteGroup()
     * path. isOwner() is "ownerId != null && ownerId.equals(userId)". This covers: ownerId null
     * (short-circuit false, even with a null caller), owner Id set but caller differs (false),
     * and caller matches (true). Only the true path reaches the cascade, in the order
     * expenses -> settlements -> group.
     */
    @Test
    public void wb5_deleteGroup_ownershipConditionBranchesGateTheCascade() {
        Group orphan = new Group("orphan", "No owner", "", null, new ArrayList<>());
        when(groupRepository.findById("orphan")).thenReturn(orphan);
        assertFalse(orphan.isOwner(null));
        PermissionException nullOwner = assertThrows(PermissionException.class,
                () -> groupService.deleteGroup(null, "orphan"));
        assertEquals("Only the group owner can do this.", nullOwner.getMessage());

        Group group = new Group(GROUP_ID, "Trip", "", OWNER,
                new ArrayList<>(Arrays.asList(OWNER, MEMBER)));
        when(groupRepository.findById(GROUP_ID)).thenReturn(group);
        assertThrows(PermissionException.class, () -> groupService.deleteGroup(MEMBER, GROUP_ID));

        verifyNoInteractions(expenseRepository, settlementRepository);
        verify(groupRepository, never()).delete(any());

        groupService.deleteGroup(OWNER, GROUP_ID);
        InOrder order = inOrder(expenseRepository, settlementRepository, groupRepository);
        order.verify(expenseRepository).deleteByGroup(GROUP_ID);
        order.verify(settlementRepository).deleteByGroup(GROUP_ID);
        order.verify(groupRepository).delete(GROUP_ID);
    }

    private void assertRegisterMessage(String expected, String name, String email, String uscId,
                                       String password) {
        ValidationException e = assertThrows(ValidationException.class,
                () -> accountService.register(name, email, uscId, password));
        assertEquals(expected, e.getMessage());
    }
}
