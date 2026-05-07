package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.common.ApiException;
import com.chatroom.entity.User;
import com.chatroom.mapper.UserMapper;
import com.chatroom.service.UserService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        return userMapper.selectList(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new ApiException("用户不存在");
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String signature) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new ApiException("用户不存在");
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (signature != null && !signature.isBlank()) user.setSignature(signature);
        userMapper.updateById(user);
    }
}
