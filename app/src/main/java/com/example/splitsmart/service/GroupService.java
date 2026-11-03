package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;
import com.example.splitsmart.repository.SettlementRepository;
import com.example.splitsmart.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Creating, editing and deleting groups, and managing their members (SD2). Calls the
 * repositories, so never run it on the main thread.
 */
public class GroupService {

    // TODO(Q2): replace with exact Project 2.1 text
    private static final String ERROR_NAME = "Enter a group name.";
    private static final String ERROR_NO_GROUP = "This group no longer exists.";
    private static final String ERROR_NOT_OWNER = "Only the group owner can do this.";
    private static final String ERROR_UNKNOWN_EMAIL = "No account was found with this email.";
    private static final String ERROR_ALREADY_MEMBER = "This user is already a member of the group.";

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository,
                        ExpenseRepository expenseRepository,
                        SettlementRepository settlementRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    /**
     * Creates a group. The creator becomes its owner and first member.
     *
     * @param ownerId the ID of the user creating the group
     * @param name the group name
     * @param desc the group description
     * @return the new group
     * @throws ValidationException if the name is empty
     */
    public Group createGroup(String ownerId, String name, String desc) {
        String trimmedName = trim(name);
        if (trimmedName.isEmpty()) {
            throw new ValidationException(ERROR_NAME);
        }

        List<String> memberIds = new ArrayList<>();
        memberIds.add(ownerId);
        Group group = new Group(null, trimmedName, trim(desc), ownerId, memberIds);
        groupRepository.save(group);
        return group;
    }

    /**
     * Adds an existing user to a group.
     *
     * @param callerId the ID of the user making the change
     * @param groupId the ID of the group
     * @param email the email of the user to add
     * @throws ValidationException if the group does not exist, the email is unknown, or the
     *                             user is already a member
     * @throws PermissionException if the caller is not the group owner
     */
    public void addMemberByEmail(String callerId, String groupId, String email) {
        Group group = findGroupOwnedBy(callerId, groupId);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ValidationException(ERROR_UNKNOWN_EMAIL);
        }
        if (group.isMember(user.getUserId())) {
            throw new ValidationException(ERROR_ALREADY_MEMBER);
        }

        List<String> memberIds = new ArrayList<>(group.getMemberIds());
        memberIds.add(user.getUserId());
        group.setMemberIds(memberIds);
        groupRepository.update(group);
    }

    /**
     * Changes a group's name and description.
     *
     * @param callerId the ID of the user making the change
     * @param groupId the ID of the group
     * @param name the new name
     * @param desc the new description
     * @throws ValidationException if the group does not exist or the name is empty
     * @throws PermissionException if the caller is not the group owner
     */
    public void updateGroup(String callerId, String groupId, String name, String desc) {
        Group group = findGroupOwnedBy(callerId, groupId);
        String trimmedName = trim(name);
        if (trimmedName.isEmpty()) {
            throw new ValidationException(ERROR_NAME);
        }

        group.setName(trimmedName);
        group.setDescription(trim(desc));
        groupRepository.update(group);
    }

    /**
     * Deletes a group with its expenses and settlements. The group goes last: the Firestore
     * rules read it to let the owner delete other members' records, and a cascade that fails
     * partway can then be retried.
     *
     * @param callerId the ID of the user making the change
     * @param groupId the ID of the group
     * @throws ValidationException if the group does not exist
     * @throws PermissionException if the caller is not the group owner
     */
    public void deleteGroup(String callerId, String groupId) {
        findGroupOwnedBy(callerId, groupId);

        expenseRepository.deleteByGroup(groupId);
        settlementRepository.deleteByGroup(groupId);
        groupRepository.delete(groupId);
    }

    /**
     * Gets every group a user is in.
     *
     * @param userId the ID of the user
     * @return the user's groups, or an empty list if they are in none
     */
    public List<Group> getGroupsForUser(String userId) {
        return groupRepository.findByMember(userId);
    }

    /**
     * Gets the members of a group, leaving out any member ID with no user profile.
     *
     * @param groupId the ID of the group
     * @return the group's members, in member list order
     * @throws ValidationException if the group does not exist
     */
    public List<User> getMembers(String groupId) {
        Group group = findGroup(groupId);
        List<User> members = new ArrayList<>();
        for (String memberId : group.getMemberIds()) {
            User member = userRepository.findById(memberId);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    private Group findGroup(String groupId) {
        Group group = groupRepository.findById(groupId);
        if (group == null) {
            throw new ValidationException(ERROR_NO_GROUP);
        }
        return group;
    }

    private Group findGroupOwnedBy(String callerId, String groupId) {
        Group group = findGroup(groupId);
        if (!group.isOwner(callerId)) {
            throw new PermissionException(ERROR_NOT_OWNER);
        }
        return group;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
