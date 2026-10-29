package com.example.splitsmart.repository;

import com.example.splitsmart.model.Settlement;

import java.util.List;

public interface SettlementRepository {

    /**
     * Saves a new settlement.
     *
     * @param settlement the settlement to save
     */
    void save(Settlement settlement);

    /**
     * Updates an existing settlement.
     *
     * @param settlement the settlement to update
     */
    void update(Settlement settlement);

    /**
     * Deletes a settlement by ID.
     *
     * @param settlementId the ID of the settlement to delete
     */
    void delete(String settlementId);

    /**
     * Finds a settlement by its ID.
     *
     * @param settlementId the ID of the settlement to find
     * @return the Settlement if found, or null if no settlement matches
     */
    Settlement findById(String settlementId);

    /**
     * Finds all settlements belonging to a specific group.
     *
     * @param groupId the ID of the group
     * @return list of settlements, or empty list if none found
     */
    List<Settlement> findByGroup(String groupId);

    /**
     * Deletes all settlements associated with a group.
     *
     * @param groupId the ID of the group whose settlements will be deleted
     */
    void deleteByGroup(String groupId);
}
