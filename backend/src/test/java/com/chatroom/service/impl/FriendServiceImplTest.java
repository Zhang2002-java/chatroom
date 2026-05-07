package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.FriendRelation;
import com.chatroom.entity.User;
import com.chatroom.mapper.FriendRelationMapper;
import com.chatroom.mapper.UserMapper;
import com.chatroom.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock private FriendRelationMapper friendRelationMapper;
    @Mock private UserMapper userMapper;
    @Mock private ChatWebSocketHandler wsHandler;

    @InjectMocks
    private FriendServiceImpl friendService;

    private User sender;
    private User target;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L); sender.setUsername("alice"); sender.setNickname("Alice"); sender.setAvatar("a.png");

        target = new User();
        target.setId(2L); target.setUsername("bob"); target.setNickname("Bob"); target.setAvatar("b.png");
    }

    @Test
    void sendRequest_self_throws() {
        ApiException ex = assertThrows(ApiException.class, () -> friendService.sendRequest(1L, 1L));
        assertEquals("不能添加自己为好友", ex.getMessage());
    }

    @Test
    void sendRequest_existingPending_throws() {
        FriendRelation existing = new FriendRelation();
        existing.setStatus(0);
        when(friendRelationMapper.selectOne(any())).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> friendService.sendRequest(1L, 2L));
        assertEquals("已经发送过好友申请", ex.getMessage());
    }

    @Test
    void sendRequest_existingAccepted_throws() {
        FriendRelation existing = new FriendRelation();
        existing.setStatus(1);
        when(friendRelationMapper.selectOne(any())).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> friendService.sendRequest(1L, 2L));
        assertEquals("对方已是你的好友", ex.getMessage());
    }

    @Test
    void sendRequest_existingBlocked_throws() {
        FriendRelation existing = new FriendRelation();
        existing.setStatus(2);
        when(friendRelationMapper.selectOne(any())).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> friendService.sendRequest(1L, 2L));
        assertEquals("对方已被拉黑，无法添加", ex.getMessage());
    }

    @Test
    void sendRequest_rejectedReusesAndNotifies() {
        FriendRelation existing = new FriendRelation();
        existing.setId(10L); existing.setUserId(2L); existing.setFriendId(1L); existing.setStatus(3);
        when(friendRelationMapper.selectOne(any())).thenReturn(existing);
        when(userMapper.selectById(1L)).thenReturn(sender);

        friendService.sendRequest(1L, 2L);

        verify(friendRelationMapper).updateById(existing);
        assertEquals(0, existing.getStatus());
        assertEquals(1L, existing.getUserId());
        assertEquals(2L, existing.getFriendId());
        verify(wsHandler).notifyUser(eq(2L), any(ObjectNode.class));
    }

    @Test
    void sendRequest_newRelation_createsAndNotifies() {
        when(friendRelationMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(sender);

        friendService.sendRequest(1L, 2L);

        verify(friendRelationMapper).insert(any(FriendRelation.class));
        verify(wsHandler).notifyUser(eq(2L), any(ObjectNode.class));
    }

    @Test
    void acceptRequest_success_notifiesRequester() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setUserId(1L); relation.setFriendId(2L); relation.setStatus(0);
        when(friendRelationMapper.selectById(10L)).thenReturn(relation);
        when(userMapper.selectById(2L)).thenReturn(target);

        friendService.acceptRequest(10L, 2L);

        assertEquals(1, relation.getStatus());
        verify(friendRelationMapper).updateById(relation);
        verify(wsHandler).notifyUser(eq(1L), any(ObjectNode.class));
    }

    @Test
    void acceptRequest_throws_whenNotFound() {
        when(friendRelationMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> friendService.acceptRequest(99L, 1L));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void acceptRequest_throws_whenNotTarget() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setUserId(1L); relation.setFriendId(2L);
        when(friendRelationMapper.selectById(10L)).thenReturn(relation);

        ApiException ex = assertThrows(ApiException.class, () -> friendService.acceptRequest(10L, 3L));
        assertEquals("无权操作此申请", ex.getMessage());
    }

    @Test
    void rejectRequest_setsStatusToRejected() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setUserId(1L); relation.setFriendId(2L); relation.setStatus(0);
        when(friendRelationMapper.selectById(10L)).thenReturn(relation);

        friendService.rejectRequest(10L, 2L);

        assertEquals(3, relation.getStatus());
        verify(friendRelationMapper).updateById(relation);
    }

    @Test
    void deleteFriend_deletesById() {
        friendService.deleteFriend(10L, 1L);
        verify(friendRelationMapper).deleteById(10L);
    }

    @Test
    void blockFriend_setsStatusToBlocked() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setStatus(1);
        when(friendRelationMapper.selectById(10L)).thenReturn(relation);

        friendService.blockFriend(10L, 1L);

        assertEquals(2, relation.getStatus());
        verify(friendRelationMapper).updateById(relation);
    }

    @Test
    void blockFriend_throws_whenNotFound() {
        when(friendRelationMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> friendService.blockFriend(99L, 1L));
        assertEquals("好友关系不存在", ex.getMessage());
    }

    @Test
    void getFriendList_returnsAcceptedFriends() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setUserId(1L); relation.setFriendId(2L); relation.setStatus(1);
        when(friendRelationMapper.selectList(any())).thenReturn(List.of(relation));
        when(userMapper.selectById(2L)).thenReturn(target);

        var result = friendService.getFriendList(1L);

        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).get("nickname"));
    }

    @Test
    void getFriendList_returnsEmpty_whenNoAcceptedFriends() {
        when(friendRelationMapper.selectList(any())).thenReturn(List.of());
        assertTrue(friendService.getFriendList(1L).isEmpty());
    }

    @Test
    void getPendingRequests_returnsIncomingOnly() {
        FriendRelation relation = new FriendRelation();
        relation.setId(10L); relation.setUserId(1L); relation.setFriendId(2L); relation.setStatus(0);
        when(friendRelationMapper.selectList(any())).thenReturn(List.of(relation));
        when(userMapper.selectById(1L)).thenReturn(sender);

        var result = friendService.getPendingRequests(2L);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).get("nickname"));
    }
}
