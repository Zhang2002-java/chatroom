package com.chatroom.service.impl;

import com.chatroom.common.ApiException;
import com.chatroom.dto.LoginRequest;
import com.chatroom.dto.RegisterRequest;
import com.chatroom.entity.User;
import com.chatroom.mapper.UserMapper;
import com.chatroom.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_success_whenUsernameNotTaken() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");

        authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertEquals("testuser", inserted.getUsername());
        assertEquals("encoded-pass", inserted.getPassword());
        assertEquals("testuser", inserted.getNickname());
    }

    @Test
    void register_throws_whenUsernameExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));
        assertEquals("用户名已存在", ex.getMessage());
        verify(userMapper, never()).insert(Collections.singleton(any()));
    }

    @Test
    void login_success_returnsLoginResponse() {
        User foundUser = new User();
        foundUser.setId(1L);
        foundUser.setUsername("testuser");
        foundUser.setPassword("encoded");
        foundUser.setNickname("Test");
        foundUser.setAvatar("/avatar.png");

        when(userMapper.selectOne(any())).thenReturn(foundUser);
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        var response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test", response.getNickname());
        assertEquals("/avatar.png", response.getAvatar());
    }

    @Test
    void login_throws_whenUserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(loginRequest));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_throws_whenPasswordMismatch() {
        User foundUser = new User();
        foundUser.setPassword("encoded");
        when(userMapper.selectOne(any())).thenReturn(foundUser);
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(loginRequest));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_throws_whenUsernameEmpty() {
        loginRequest.setUsername("");
        when(userMapper.selectOne(any())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(loginRequest));
        assertEquals("用户名或密码错误", ex.getMessage());
    }
}
