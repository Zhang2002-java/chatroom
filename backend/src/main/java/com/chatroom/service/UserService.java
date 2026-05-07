package com.chatroom.service;

import com.chatroom.entity.User;
import java.util.List;

public interface UserService {
    List<User> searchUsers(String keyword);
    User getUserById(Long id);
    void updateProfile(Long userId, String nickname, String signature);
}
