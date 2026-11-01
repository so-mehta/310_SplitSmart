package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;
import com.example.splitsmart.repository.SettlementRepository;
import com.example.splitsmart.repository.UserRepository;

import java.util.List;

/**
 * Creating, editing and deleting groups, and managing their members (SD2). Calls the
 * repositories, so never run it on the main thread.
 */
public class GroupService {

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
        throw new UnsupportedOperationException("Not implemented yet");
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
        throw new UnsupportedOperationException("Not implemented yet");
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Deletes a group with its expenses and settlements.
     *
     * @param callerId the ID of the user making the change
     * @param groupId the ID of the group
     * @throws ValidationException if the group does not exist
     * @throws PermissionException if the caller is not the group owner
     */
    public void deleteGroup(String callerId, String groupId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Gets every group a user is in.
     *
     * @param userId the ID of the user
     * @return the user's groups, or an empty list if they are in none
     */
    public List<Group> getGroupsForUser(String userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Gets the members of a group.
     *
     * @param groupId the ID of the group
     * @return the group's members
     * @throws ValidationException if the group does not exist
     */
    public List<User> getMembers(String groupId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
