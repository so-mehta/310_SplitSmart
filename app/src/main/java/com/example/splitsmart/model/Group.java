package com.example.splitsmart.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String groupId;
    private String name;
    private String description;
    private String ownerId;
    private List<String> memberIds;

    public Group() {
        this.memberIds = new ArrayList<>();
    }

    public Group(String groupId, String name, String description, String ownerId, List<String> memberIds) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public boolean isOwner(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isMember(String userId) {
        return memberIds != null && memberIds.contains(userId);
    }
}
