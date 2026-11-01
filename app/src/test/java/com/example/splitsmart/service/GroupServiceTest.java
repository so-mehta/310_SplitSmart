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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    private static final String GROUP_ID = "group-1";
    private static final String OWNER = "owner";
    private static final String MEMBER = "member";
    private static final String OUTSIDER = "outsider";
    private static final String NEW_MEMBER = "new-member";
    private static final String NEW_EMAIL = "new@usc.edu";

    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private GroupService groupService;
    private Group group;

    @Before
    public void setUp() {
        groupRepository = mock(GroupRepository.class);
        userRepository = mock(UserRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        groupService = new GroupService(groupRepository, userRepository, expenseRepository,
                settlementRepository);

        group = new Group(GROUP_ID, "Apartment", "Rent and bills", OWNER,
                new ArrayList<>(Arrays.asList(OWNER, MEMBER)));
        when(groupRepository.findById(GROUP_ID)).thenReturn(group);
    }

    @Test
    public void createGroup_makesTheCreatorOwnerAndFirstMember() {
        Group created = groupService.createGroup(OWNER, "Trip", "Spring break");

        assertEquals("Trip", created.getName());
        assertEquals("Spring break", created.getDescription());
        assertEquals(OWNER, created.getOwnerId());
        assertEquals(Collections.singletonList(OWNER), created.getMemberIds());
        verify(groupRepository).save(created);
    }

    @Test
    public void createGroup_memberListCanGrowLater() {
        Group created = groupService.createGroup(OWNER, "Trip", "Spring break");

        created.getMemberIds().add(MEMBER);

        assertTrue(created.isMember(MEMBER));
    }

    @Test
    public void createGroup_trimsNameAndDescription() {
        Group created = groupService.createGroup(OWNER, "  Trip ", " Spring break  ");

        assertEquals("Trip", created.getName());
        assertEquals("Spring break", created.getDescription());
    }

    @Test
    public void createGroup_descriptionIsOptional() {
        Group created = groupService.createGroup(OWNER, "Trip", null);

        assertEquals("", created.getDescription());
    }

    @Test
    public void createGroup_emptyName_isRejected() {
        assertThrows(ValidationException.class, () -> groupService.createGroup(OWNER, "", "desc"));
        assertThrows(ValidationException.class, () -> groupService.createGroup(OWNER, "   ", "desc"));
        assertThrows(ValidationException.class, () -> groupService.createGroup(OWNER, null, "desc"));

        assertNoWrites();
    }

    @Test
    public void addMemberByEmail_addsTheUserAndKeepsExistingMembers() {
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(user(NEW_MEMBER));

        groupService.addMemberByEmail(OWNER, GROUP_ID, NEW_EMAIL);

        ArgumentCaptor<Group> updated = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).update(updated.capture());
        assertEquals(Arrays.asList(OWNER, MEMBER, NEW_MEMBER), updated.getValue().getMemberIds());
    }

    @Test
    public void addMemberByEmail_worksWhenTheStoredListCannotBeChanged() {
        group.setMemberIds(Collections.singletonList(OWNER));
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(user(NEW_MEMBER));

        groupService.addMemberByEmail(OWNER, GROUP_ID, NEW_EMAIL);

        verify(groupRepository).update(group);
        assertEquals(Arrays.asList(OWNER, NEW_MEMBER), group.getMemberIds());
    }

    @Test
    public void addMemberByEmail_byNonOwnerMember_isRejectedBeforeLookingUpTheEmail() {
        assertThrows(PermissionException.class,
                () -> groupService.addMemberByEmail(MEMBER, GROUP_ID, NEW_EMAIL));

        verify(userRepository, never()).findByEmail(any());
        assertNoWrites();
    }

    @Test
    public void addMemberByEmail_byNonMember_isRejected() {
        assertThrows(PermissionException.class,
                () -> groupService.addMemberByEmail(OUTSIDER, GROUP_ID, NEW_EMAIL));

        assertNoWrites();
    }

    @Test
    public void addMemberByEmail_missingGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> groupService.addMemberByEmail(OWNER, GROUP_ID, NEW_EMAIL));

        assertNoWrites();
    }

    @Test
    public void addMemberByEmail_unknownEmail_isRejected() {
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> groupService.addMemberByEmail(OWNER, GROUP_ID, NEW_EMAIL));

        assertNoWrites();
    }

    @Test
    public void addMemberByEmail_existingMember_isRejected() {
        when(userRepository.findByEmail("member@usc.edu")).thenReturn(user(MEMBER));

        assertThrows(ValidationException.class,
                () -> groupService.addMemberByEmail(OWNER, GROUP_ID, "member@usc.edu"));

        assertNoWrites();
    }

    @Test
    public void updateGroup_changesNameAndDescriptionOnly() {
        groupService.updateGroup(OWNER, GROUP_ID, "  New name ", " New desc ");

        verify(groupRepository).update(group);
        assertEquals("New name", group.getName());
        assertEquals("New desc", group.getDescription());
        assertEquals(OWNER, group.getOwnerId());
        assertEquals(Arrays.asList(OWNER, MEMBER), group.getMemberIds());
    }

    @Test
    public void updateGroup_byNonOwner_isRejected() {
        assertThrows(PermissionException.class,
                () -> groupService.updateGroup(MEMBER, GROUP_ID, "New name", "New desc"));

        assertEquals("Apartment", group.getName());
        assertNoWrites();
    }

    @Test
    public void updateGroup_missingGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> groupService.updateGroup(OWNER, GROUP_ID, "New name", "New desc"));

        assertNoWrites();
    }

    @Test
    public void updateGroup_emptyName_isRejected() {
        assertThrows(ValidationException.class,
                () -> groupService.updateGroup(OWNER, GROUP_ID, "   ", "New desc"));

        assertEquals("Apartment", group.getName());
        assertNoWrites();
    }

    @Test
    public void deleteGroup_deletesExpensesThenSettlementsThenTheGroup() {
        groupService.deleteGroup(OWNER, GROUP_ID);

        InOrder order = inOrder(expenseRepository, settlementRepository, groupRepository);
        order.verify(expenseRepository).deleteByGroup(GROUP_ID);
        order.verify(settlementRepository).deleteByGroup(GROUP_ID);
        order.verify(groupRepository).delete(GROUP_ID);
    }

    @Test
    public void deleteGroup_keepsTheGroupIfTheCascadeFails() {
        doThrow(new RuntimeException("offline")).when(settlementRepository).deleteByGroup(GROUP_ID);

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> groupService.deleteGroup(OWNER, GROUP_ID));

        assertEquals("offline", failure.getMessage());
        verify(groupRepository, never()).delete(any());
    }

    @Test
    public void deleteGroup_byNonOwner_isRejected() {
        assertThrows(PermissionException.class, () -> groupService.deleteGroup(MEMBER, GROUP_ID));

        assertNoWrites();
    }

    @Test
    public void deleteGroup_missingGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> groupService.deleteGroup(OWNER, GROUP_ID));

        assertNoWrites();
    }

    @Test
    public void getGroupsForUser_returnsEveryGroupTheUserIsIn() {
        List<Group> groups = Arrays.asList(group, new Group());
        when(groupRepository.findByMember(MEMBER)).thenReturn(groups);

        assertSame(groups, groupService.getGroupsForUser(MEMBER));
    }

    @Test
    public void getGroupsForUser_isEmptyForAUserInNoGroups() {
        when(groupRepository.findByMember(OUTSIDER)).thenReturn(Collections.emptyList());

        assertTrue(groupService.getGroupsForUser(OUTSIDER).isEmpty());
    }

    @Test
    public void getMembers_returnsUsersInMemberOrder() {
        User owner = user(OWNER);
        User member = user(MEMBER);
        when(userRepository.findById(OWNER)).thenReturn(owner);
        when(userRepository.findById(MEMBER)).thenReturn(member);

        assertEquals(Arrays.asList(owner, member), groupService.getMembers(GROUP_ID));
    }

    @Test
    public void getMembers_skipsIdsWithNoProfile() {
        User owner = user(OWNER);
        when(userRepository.findById(OWNER)).thenReturn(owner);
        when(userRepository.findById(MEMBER)).thenReturn(null);

        assertEquals(Collections.singletonList(owner), groupService.getMembers(GROUP_ID));
    }

    @Test
    public void getMembers_missingGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> groupService.getMembers(GROUP_ID));
    }

    private void assertNoWrites() {
        verify(groupRepository, never()).save(any());
        verify(groupRepository, never()).update(any());
        verify(groupRepository, never()).delete(any());
        verify(expenseRepository, never()).deleteByGroup(any());
        verify(settlementRepository, never()).deleteByGroup(any());
    }

    private static User user(String userId) {
        return new User(userId, userId, userId + "@usc.edu", "1234567890");
    }
}
