package com.example.splitsmart.repository;

import com.example.splitsmart.model.Group;

import java.util.List;

public interface GroupRepository {

    /**
     * Saves a new group.
     *
     * @param group the group to save
     */
    void save(Group group);

    /**
     * Updates an existing group.
     *
     * @param group the group to update
     */
    void update(Group group);

    /**
     * Deletes a group by ID.
     *
     * @param groupId the ID of the group to delete
     */
    void delete(String groupId);

    /**
     * Finds a group by its ID.
     *
     * @param groupId the ID of the group to find
     * @return the Group if found, or null if no group matches
     */
    Group findById(String groupId);

    /**
     * Finds all groups that contain the specified user ID as a member.
     *
     * @param userId the member user ID to filter by
     * @return list of groups, or empty list if none found
     */
    List<Group> findByMember(String userId);
}
