package com.chatroom.service;

import com.chatroom.entity.GroupInfo;
import java.util.List;
import java.util.Map;

public interface GroupService {
    GroupInfo createGroup(String name, Long ownerId);
    void addMember(Long groupId, Long userId, Long operatorId);
    void removeMember(Long groupId, Long userId, Long operatorId);
    List<Map<String, Object>> getMyGroups(Long userId);
    List<Map<String, Object>> getMembers(Long groupId);
}
