package com.example.splitsmart.repository;

import com.example.splitsmart.model.Group;
import com.example.splitsmart.service.ResultCallback;

import java.util.List;

public interface GroupRepository {

    /**
     * Saves a new group.
     * Calls {@code callback.onSuccess(null)} when saved successfully.
     *
     * @param group the group to save
     * @param callback result callback
     */
    void save(Group group, ResultCallback<Void> callback);

    /**
     * Updates an existing group.
     * Calls {@code callback.onSuccess(null)} when updated successfully.
     *
     * @param group the group to update
     * @param callback result callback
     */
    void update(Group group, ResultCallback<Void> callback);

    /**
     * Deletes a group by ID.
     * Calls {@code callback.onSuccess(null)} when deleted successfully.
     *
     * @param groupId the ID of the group to delete
     * @param callback result callback
     */
    void delete(String groupId, ResultCallback<Void> callback);

    /**
     * Finds a group by its ID.
     * Calls {@code callback.onSuccess(Group)} if found, or {@code callback.onSuccess(null)} if no group matches.
     *
     * @param groupId the ID of the group to find
     * @param callback result callback receiving the group or null if not found
     */
    void findById(String groupId, ResultCallback<Group> callback);

    /**
     * Finds all groups that contain the specified user ID as a member.
     * Calls {@code callback.onSuccess(List<Group>)} containing matching groups, or {@code callback.onSuccess(empty list)} if no groups are found.
     *
     * @param userId the member user ID to filter by
     * @param callback result callback receiving list of groups
     */
    void findByMember(String userId, ResultCallback<List<Group>> callback);
}
