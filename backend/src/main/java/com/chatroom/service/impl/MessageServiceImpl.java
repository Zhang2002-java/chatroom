package com.chatroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.common.ApiException;
import com.chatroom.entity.Message;
import com.chatroom.mapper.MessageMapper;
import com.chatroom.service.MessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public Page<Message> getMessages(Long targetId, String chatType, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getChatType, chatType);

        if ("private".equals(chatType)) {
            wrapper.and(w -> w
                .or(w2 -> w2.eq(Message::getSenderId, currentUserId).eq(Message::getReceiverId, targetId))
                .or(w2 -> w2.eq(Message::getSenderId, targetId).eq(Message::getReceiverId, currentUserId))
            );
        } else {
            wrapper.eq(Message::getReceiverId, targetId);
        }

        wrapper.orderByDesc(Message::getCreatedAt);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Message> searchMessages(String keyword, Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getSenderId, currentUserId).or().eq(Message::getReceiverId, currentUserId))
               .like(Message::getContent, keyword)
               .eq(Message::getContentType, "text")
               .orderByDesc(Message::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public void recallMessage(Long messageId, Long currentUserId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) throw new ApiException("消息不存在");
        if (!msg.getSenderId().equals(currentUserId)) throw new ApiException("只能撤回自己发送的消息");
        if (msg.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new ApiException("只能撤回2分钟内的消息");
        }
        msg.setIsRecalled(1);
        messageMapper.updateById(msg);
    }
}
