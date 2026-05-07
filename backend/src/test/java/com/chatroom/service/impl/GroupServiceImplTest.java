package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.GroupInfo;
import com.chatroom.entity.GroupMember;
import com.chatroom.entity.User;
import com.chatroom.mapper.GroupInfoMapper;
import com.chatroom.mapper.GroupMemberMapper;
import com.chatroom.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock private GroupInfoMapper groupInfoMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Test
    void createGroup_createsGroupAndAddsOwnerAsMember() {
        when(groupInfoMapper.insert(any(GroupInfo.class))).thenAnswer(inv -> {
            GroupInfo g = inv.getArgument(0);
            g.setId(1L);
            return 1;
        });

        GroupInfo result = groupService.createGroup("TestGroup", 1L);

        assertEquals("TestGroup", result.getName());
        assertEquals(1L, result.getOwnerId());
        assertEquals(1L, result.getId());
        verify(groupMemberMapper).insert(any(GroupMember.class));
    }

    @Test
    void addMember_success() {
        GroupInfo group = new GroupInfo(); group.setId(1L);
        when(groupInfoMapper.selectById(1L)).thenReturn(group);
        when(groupMemberMapper.selectCount(any())).thenReturn(0L);

        groupService.addMember(1L, 2L, 1L);

        verify(groupMemberMapper).insert(any(GroupMember.class));
    }

    @Test
    void addMember_throws_whenGroupNotFound() {
        when(groupInfoMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> groupService.addMember(99L, 2L, 1L));
        assertEquals("群组不存在", ex.getMessage());
    }

    @Test
    void addMember_throws_whenAlreadyInGroup() {
        GroupInfo group = new GroupInfo(); group.setId(1L);
        when(groupInfoMapper.selectById(1L)).thenReturn(group);
        when(groupMemberMapper.selectCount(any())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> groupService.addMember(1L, 2L, 1L));
        assertEquals("用户已在群中", ex.getMessage());
    }

    @Test
    void removeMember_selfRemoval_success() {
        GroupMember member = new GroupMember();
        member.setId(1L); member.setGroupId(1L); member.setUserId(2L); member.setRole("member");
        when(groupMemberMapper.selectOne(any())).thenReturn(member);

        groupService.removeMember(1L, 2L, 2L);

        verify(groupMemberMapper).deleteById(member);
    }

    @Test
    void removeMember_throws_whenNotInGroup() {
        when(groupMemberMapper.selectOne(any())).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> groupService.removeMember(1L, 3L, 1L));
        assertEquals("成员不在群中", ex.getMessage());
    }

    @Test
    void removeMember_throws_whenRemovingOwner() {
        GroupMember owner = new GroupMember();
        owner.setId(1L); owner.setGroupId(1L); owner.setUserId(1L); owner.setRole("owner");
        when(groupMemberMapper.selectOne(any())).thenReturn(owner);

        ApiException ex = assertThrows(ApiException.class, () -> groupService.removeMember(1L, 1L, 2L));
        assertEquals("不能移除群主", ex.getMessage());
    }

    @Test
    void getMyGroups_returnsGroups() {
        GroupMember member = new GroupMember();
        member.setGroupId(1L); member.setUserId(1L); member.setRole("member");
        GroupInfo group = new GroupInfo();
        group.setId(1L); group.setName("Test"); group.setOwnerId(1L);
        when(groupMemberMapper.selectList(any())).thenReturn(List.of(member));
        when(groupInfoMapper.selectById(1L)).thenReturn(group);

        var result = groupService.getMyGroups(1L);

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).get("name"));
    }

    @Test
    void getMembers_returnsMemberList() {
        GroupMember member = new GroupMember();
        member.setGroupId(1L); member.setUserId(2L); member.setRole("member");
        User user = new User();
        user.setId(2L); user.setNickname("Bob"); user.setAvatar("b.png");
        when(groupMemberMapper.selectList(any())).thenReturn(List.of(member));
        when(userMapper.selectById(2L)).thenReturn(user);

        var result = groupService.getMembers(1L);

        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).get("nickname"));
    }
}
