package com.chatroom.service;

import java.util.List;
import java.util.Map;

public interface FriendService {
    void sendRequest(Long userId, Long friendId);
    void acceptRequest(Long relationId, Long currentUserId);
    void rejectRequest(Long relationId, Long currentUserId);
    void deleteFriend(Long relationId, Long currentUserId);
    void blockFriend(Long relationId, Long currentUserId);
    void unblockFriend(Long relationId, Long currentUserId);
    List<Map<String, Object>> getFriendList(Long userId);
    List<Map<String, Object>> getPendingRequests(Long userId);
    List<Map<String, Object>> getBlockedList(Long userId);
}
