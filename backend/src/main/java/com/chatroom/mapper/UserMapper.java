package com.chatroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatroom.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
