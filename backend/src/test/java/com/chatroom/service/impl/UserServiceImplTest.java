package com.chatroom.service.impl;

import com.chatroom.common.ApiException;
import com.chatroom.entity.User;
import com.chatroom.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void searchUsers_returnsFilteredList() {
        User u1 = new User(); u1.setUsername("alice");
        when(userMapper.selectList(any())).thenReturn(List.of(u1));

        var result = userService.searchUsers("ali");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
    }

    @Test
    void searchUsers_returnsEmptyList_whenNoMatches() {
        when(userMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, userService.searchUsers("nonexistent").size());
    }

    @Test
    void getUserById_returnsUserWithNullPassword() {
        User user = new User();
        user.setId(1L); user.setUsername("test"); user.setPassword("secret");
        when(userMapper.selectById(1L)).thenReturn(user);

        User result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        assertNull(result.getPassword());
    }

    @Test
    void getUserById_throws_whenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> userService.getUserById(99L));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void updateProfile_updatesBothFields() {
        User user = new User(); user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, "new-nick", "new-sig");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("new-nick", captor.getValue().getNickname());
        assertEquals("new-sig", captor.getValue().getSignature());
    }

    @Test
    void updateProfile_skipsBlankFields() {
        User user = new User(); user.setId(1L); user.setNickname("old"); user.setSignature("old-sig");
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, "", null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("old", captor.getValue().getNickname());
        assertEquals("old-sig", captor.getValue().getSignature());
    }

    @Test
    void updateProfile_throws_whenUserNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> userService.updateProfile(99L, "nick", "sig"));
        assertEquals("用户不存在", ex.getMessage());
    }
}
