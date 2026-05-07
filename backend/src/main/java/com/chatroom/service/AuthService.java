package com.chatroom.service;

import com.chatroom.dto.LoginRequest;
import com.chatroom.dto.LoginResponse;
import com.chatroom.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
