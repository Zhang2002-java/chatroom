package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.*;
import com.chatroom.mapper.*;
import com.chatroom.service.GroupService;
import com.chatroom.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ChatWebSocketHandler wsHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroupServiceImpl(GroupInfoMapper gim, GroupMemberMapper gmm, UserMapper um, ChatWebSocketHandler wsh) {
        this.groupInfoMapper = gim; this.groupMemberMapper = gmm; this.userMapper = um; this.wsHandler = wsh;
    }

    @Override
    public GroupInfo createGroup(String name, Long ownerId) {
        GroupInfo group = new GroupInfo();
        group.setName(name); group.setOwnerId(ownerId);
        groupInfoMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId()); member.setUserId(ownerId); member.setRole("owner");
        groupMemberMapper.insert(member);
        return group;
    }

    @Override
    public void addMember(Long groupId, Long userId, Long operatorId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) throw new ApiException("群组不存在");

        LambdaQueryWrapper<GroupMember> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, operatorId);
        if (groupMemberMapper.selectCount(opWrapper) == 0) throw new ApiException("只有群成员才能邀请他人");

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(wrapper) > 0) throw new ApiException("用户已在群中");

        GroupMember member = new GroupMember();
        member.setGroupId(groupId); member.setUserId(userId); member.setRole("member");
        groupMemberMapper.insert(member);

        User opUser = userMapper.selectById(operatorId);
        User newUser = userMapper.selectById(userId);
        ObjectNode notification = objectMapper.createObjectNode()
                .put("type", "GROUP_MEMBER_ADDED")
                .put("groupId", groupId)
                .put("groupName", group.getName())
                .put("operatorId", operatorId)
                .put("operatorNickname", opUser != null ? opUser.getNickname() : "")
                .put("userId", userId)
                .put("nickname", newUser != null ? newUser.getNickname() : "")
                .put("avatar", newUser != null ? newUser.getAvatar() : "");
        wsHandler.notifyGroup(groupId, notification);
    }

    @Override
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        GroupMember member = groupMemberMapper.selectOne(wrapper);
        if (member == null) throw new ApiException("成员不在群中");

        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) throw new ApiException("群组不存在");

        boolean isSelfLeave = userId.equals(operatorId);
        boolean isOwnerOperate = group.getOwnerId().equals(operatorId);

        if (!isSelfLeave && !isOwnerOperate) throw new ApiException("只有群主可以移除成员");
        if (!isSelfLeave && "owner".equals(member.getRole())) throw new ApiException("不能移除群主");

        groupMemberMapper.deleteById(member);

        User opUser = userMapper.selectById(operatorId);
        User removedUser = userMapper.selectById(userId);
        ObjectNode notification = objectMapper.createObjectNode()
                .put("type", "GROUP_MEMBER_REMOVED")
                .put("groupId", groupId)
                .put("groupName", group.getName())
                .put("operatorId", operatorId)
                .put("operatorNickname", opUser != null ? opUser.getNickname() : "")
                .put("userId", userId)
                .put("nickname", removedUser != null ? removedUser.getNickname() : "");
        wsHandler.notifyGroup(groupId, notification);
    }

    @Override
    public void deleteGroup(Long groupId, Long operatorId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) throw new ApiException("群组不存在");
        if (!group.getOwnerId().equals(operatorId)) throw new ApiException("只有群主可以解散群组");

        List<GroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));

        groupMemberMapper.delete(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));
        groupInfoMapper.deleteById(groupId);

        ObjectNode notification = objectMapper.createObjectNode()
                .put("type", "GROUP_DELETED")
                .put("groupId", groupId)
                .put("groupName", group.getName());
        for (GroupMember member : members) {
            wsHandler.notifyUser(member.getUserId(), notification);
        }
    }

    @Override
    public List<Map<String, Object>> getMyGroups(Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getUserId, userId);
        List<GroupMember> memberships = groupMemberMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember m : memberships) {
            GroupInfo group = groupInfoMapper.selectById(m.getGroupId());
            if (group != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("groupId", group.getId()); map.put("name", group.getName());
                map.put("avatar", group.getAvatar()); map.put("ownerId", group.getOwnerId());
                map.put("role", m.getRole());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember m : members) {
            User user = userMapper.selectById(m.getUserId());
            if (user != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getId()); map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar()); map.put("role", m.getRole());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> inviteFriends(Long groupId, List<Long> friendIds, Long operatorId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) throw new ApiException("群组不存在");

        LambdaQueryWrapper<GroupMember> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, operatorId);
        if (groupMemberMapper.selectCount(opWrapper) == 0) throw new ApiException("只有群成员才能邀请好友");

        User opUser = userMapper.selectById(operatorId);
        int addedCount = 0;
        List<String> skipped = new ArrayList<>();

        for (Long friendId : friendIds) {
            LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, friendId);
            if (groupMemberMapper.selectCount(wrapper) > 0) {
                User u = userMapper.selectById(friendId);
                skipped.add(u != null ? u.getNickname() : friendId.toString());
                continue;
            }

            GroupMember member = new GroupMember();
            member.setGroupId(groupId); member.setUserId(friendId); member.setRole("member");
            groupMemberMapper.insert(member);
            addedCount++;

            User newUser = userMapper.selectById(friendId);
            ObjectNode notification = objectMapper.createObjectNode()
                    .put("type", "GROUP_MEMBER_ADDED")
                    .put("groupId", groupId)
                    .put("groupName", group.getName())
                    .put("operatorId", operatorId)
                    .put("operatorNickname", opUser != null ? opUser.getNickname() : "")
                    .put("userId", friendId)
                    .put("nickname", newUser != null ? newUser.getNickname() : "")
                    .put("avatar", newUser != null ? newUser.getAvatar() : "");
            wsHandler.notifyGroup(groupId, notification);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("addedCount", addedCount);
        result.put("skipped", skipped);
        return result;
    }
}
