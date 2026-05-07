package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.*;
import com.chatroom.mapper.*;
import com.chatroom.service.GroupService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;

    public GroupServiceImpl(GroupInfoMapper gim, GroupMemberMapper gmm, UserMapper um) {
        this.groupInfoMapper = gim; this.groupMemberMapper = gmm; this.userMapper = um;
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

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(wrapper) > 0) throw new ApiException("用户已在群中");

        GroupMember member = new GroupMember();
        member.setGroupId(groupId); member.setUserId(userId); member.setRole("member");
        groupMemberMapper.insert(member);
    }

    @Override
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, userId);
        GroupMember member = groupMemberMapper.selectOne(wrapper);
        if (member == null) throw new ApiException("成员不在群中");

        if (userId.equals(operatorId) || "owner".equals(member.getRole())) {
            if ("owner".equals(member.getRole()) && !userId.equals(operatorId)) {
                throw new ApiException("不能移除群主");
            }
            groupMemberMapper.deleteById(member);
        } else {
            throw new ApiException("无权操作");
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
}
