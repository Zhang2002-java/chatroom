package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.FriendRelation;
import com.chatroom.entity.User;
import com.chatroom.mapper.FriendRelationMapper;
import com.chatroom.mapper.UserMapper;
import com.chatroom.service.FriendService;
import com.chatroom.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRelationMapper friendRelationMapper;
    private final UserMapper userMapper;
    private final ChatWebSocketHandler wsHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FriendServiceImpl(FriendRelationMapper friendRelationMapper, UserMapper userMapper, ChatWebSocketHandler wsHandler) {
        this.friendRelationMapper = friendRelationMapper;
        this.userMapper = userMapper;
        this.wsHandler = wsHandler;
    }

    @Override
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) throw new ApiException("不能添加自己为好友");

        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .or(w2 -> w2.eq(FriendRelation::getUserId, userId).eq(FriendRelation::getFriendId, friendId))
            .or(w2 -> w2.eq(FriendRelation::getUserId, friendId).eq(FriendRelation::getFriendId, userId))
        );
        FriendRelation existing = friendRelationMapper.selectOne(wrapper);
        if (existing != null) {
            if (existing.getStatus() == 0) throw new ApiException("已经发送过好友申请");
            if (existing.getStatus() == 1) throw new ApiException("对方已是你的好友");
            if (existing.getStatus() == 2) throw new ApiException("对方已被拉黑，无法添加");
            if (existing.getStatus() == 3) {
                existing.setStatus(0);
                existing.setUserId(userId);
                existing.setFriendId(friendId);
                friendRelationMapper.updateById(existing);
                User sender = userMapper.selectById(userId);
                ObjectNode notification = objectMapper.createObjectNode()
                        .put("type", "FRIEND_REQUEST")
                        .put("relationId", existing.getId())
                        .put("userId", userId)
                        .put("nickname", sender != null ? sender.getNickname() : "")
                        .put("avatar", sender != null ? sender.getAvatar() : "");
                wsHandler.notifyUser(friendId, notification);
                return;
            }
        }

        User sender = userMapper.selectById(userId);
        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId); relation.setFriendId(friendId); relation.setStatus(0);
        friendRelationMapper.insert(relation);

        ObjectNode notification = objectMapper.createObjectNode()
                .put("type", "FRIEND_REQUEST")
                .put("relationId", relation.getId())
                .put("userId", userId)
                .put("nickname", sender != null ? sender.getNickname() : "")
                .put("avatar", sender != null ? sender.getAvatar() : "");
        wsHandler.notifyUser(friendId, notification);
    }

    @Override
    public void acceptRequest(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) throw new ApiException("无权操作此申请");
        relation.setStatus(1);
        friendRelationMapper.updateById(relation);

        User accepter = userMapper.selectById(currentUserId);
        ObjectNode notification = objectMapper.createObjectNode()
                .put("type", "FRIEND_ACCEPTED")
                .put("relationId", relationId)
                .put("userId", currentUserId)
                .put("nickname", accepter != null ? accepter.getNickname() : "")
                .put("avatar", accepter != null ? accepter.getAvatar() : "");
        wsHandler.notifyUser(relation.getUserId(), notification);
    }

    @Override
    public void rejectRequest(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("申请不存在");
        if (!relation.getFriendId().equals(currentUserId)) throw new ApiException("无权操作此申请");
        relation.setStatus(3);
        friendRelationMapper.updateById(relation);
    }

    @Override
    public void deleteFriend(Long relationId, Long currentUserId) {
        friendRelationMapper.deleteById(relationId);
    }

    @Override
    public void blockFriend(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("好友关系不存在");
        relation.setStatus(2);
        friendRelationMapper.updateById(relation);
    }

    @Override
    public List<Map<String, Object>> getFriendList(Long userId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FriendRelation::getUserId, userId).or().eq(FriendRelation::getFriendId, userId))
               .eq(FriendRelation::getStatus, 1);
        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            Long friendId = r.getUserId().equals(userId) ? r.getFriendId() : r.getUserId();
            User friend = userMapper.selectById(friendId);
            if (friend != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("relationId", r.getId()); map.put("userId", friend.getId());
                map.put("username", friend.getUsername()); map.put("nickname", friend.getNickname());
                map.put("avatar", friend.getAvatar()); map.put("signature", friend.getSignature());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public void unblockFriend(Long relationId, Long currentUserId) {
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null) throw new ApiException("好友关系不存在");
        relation.setStatus(1);
        friendRelationMapper.updateById(relation);
    }

    @Override
    public List<Map<String, Object>> getBlockedList(Long userId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FriendRelation::getUserId, userId).or().eq(FriendRelation::getFriendId, userId))
               .eq(FriendRelation::getStatus, 2);
        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            Long blockedId = r.getUserId().equals(userId) ? r.getFriendId() : r.getUserId();
            User blocked = userMapper.selectById(blockedId);
            if (blocked != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("relationId", r.getId()); map.put("userId", blocked.getId());
                map.put("username", blocked.getUsername()); map.put("nickname", blocked.getNickname());
                map.put("avatar", blocked.getAvatar());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingRequests(Long userId) {
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getFriendId, userId).eq(FriendRelation::getStatus, 0);
        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRelation r : relations) {
            User sender = userMapper.selectById(r.getUserId());
            if (sender != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("relationId", r.getId()); map.put("userId", sender.getId());
                map.put("username", sender.getUsername()); map.put("nickname", sender.getNickname());
                map.put("avatar", sender.getAvatar());
                result.add(map);
            }
        }
        return result;
    }
}
