package com.example.splitsmart.repository;

import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.service.ResultCallback;

import java.util.List;

public interface SettlementRepository {

    /**
     * Saves a new settlement.
     * Calls {@code callback.onSuccess(null)} when saved successfully.
     *
     * @param settlement the settlement to save
     * @param callback result callback
     */
    void save(Settlement settlement, ResultCallback<Void> callback);

    /**
     * Updates an existing settlement.
     * Calls {@code callback.onSuccess(null)} when updated successfully.
     *
     * @param settlement the settlement to update
     * @param callback result callback
     */
    void update(Settlement settlement, ResultCallback<Void> callback);

    /**
     * Deletes a settlement by ID.
     * Calls {@code callback.onSuccess(null)} when deleted successfully.
     *
     * @param settlementId the ID of the settlement to delete
     * @param callback result callback
     */
    void delete(String settlementId, ResultCallback<Void> callback);

    /**
     * Finds a settlement by its ID.
     * Calls {@code callback.onSuccess(Settlement)} if found, or {@code callback.onSuccess(null)} if no settlement matches.
     *
     * @param settlementId the ID of the settlement to find
     * @param callback result callback receiving settlement or null if not found
     */
    void findById(String settlementId, ResultCallback<Settlement> callback);

    /**
     * Finds all settlements belonging to a specific group.
     * Calls {@code callback.onSuccess(List<Settlement>)} containing settlements, or {@code callback.onSuccess(empty list)} if no settlements are found.
     *
     * @param groupId the ID of the group
     * @param callback result callback receiving list of settlements
     */
    void findByGroup(String groupId, ResultCallback<List<Settlement>> callback);

    /**
     * Deletes all settlements associated with a group.
     * Calls {@code callback.onSuccess(null)} when deleted successfully.
     *
     * @param groupId the ID of the group whose settlements will be deleted
     * @param callback result callback
     */
    void deleteByGroup(String groupId, ResultCallback<Void> callback);
}
