package com.chatroom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.entity.Message;

import java.util.List;

public interface MessageService {
    Page<Message> getMessages(Long targetId, String chatType, int page, int size, Long currentUserId);
    List<Message> searchMessages(String keyword, Long currentUserId);
    void recallMessage(Long messageId, Long currentUserId);
}
