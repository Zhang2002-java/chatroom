package com.chatroom.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.common.ApiException;
import com.chatroom.entity.Message;
import com.chatroom.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private MessageMapper messageMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void getMessages_private_returnsPagedMessages() {
        Page<Message> page = new Page<>(1, 20);
        page.setRecords(List.of(new Message()));
        when(messageMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = messageService.getMessages(2L, "private", 1, 20, 1L);

        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getMessages_group_returnsPagedMessages() {
        Page<Message> page = new Page<>(1, 20);
        page.setRecords(List.of(new Message()));
        when(messageMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = messageService.getMessages(10L, "group", 1, 20, 1L);

        assertEquals(1, result.getRecords().size());
    }

    @Test
    void searchMessages_returnsFilteredList() {
        Message msg = new Message(); msg.setContent("hello world");
        when(messageMapper.selectList(any())).thenReturn(List.of(msg));

        var result = messageService.searchMessages("hello", 1L);

        assertEquals(1, result.size());
        assertEquals("hello world", result.get(0).getContent());
    }

    @Test
    void recallMessage_success() {
        Message msg = new Message();
        msg.setId(1L); msg.setSenderId(1L); msg.setCreatedAt(LocalDateTime.now());
        when(messageMapper.selectById(1L)).thenReturn(msg);

        messageService.recallMessage(1L, 1L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsRecalled());
    }

    @Test
    void recallMessage_throws_whenNotFound() {
        when(messageMapper.selectById(99L)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> messageService.recallMessage(99L, 1L));
        assertEquals("消息不存在", ex.getMessage());
    }

    @Test
    void recallMessage_throws_whenNotOwner() {
        Message msg = new Message();
        msg.setId(1L); msg.setSenderId(2L); msg.setCreatedAt(LocalDateTime.now());
        when(messageMapper.selectById(1L)).thenReturn(msg);

        ApiException ex = assertThrows(ApiException.class, () -> messageService.recallMessage(1L, 1L));
        assertEquals("只能撤回自己发送的消息", ex.getMessage());
    }

    @Test
    void recallMessage_throws_whenExceeds2Minutes() {
        Message msg = new Message();
        msg.setId(1L); msg.setSenderId(1L); msg.setCreatedAt(LocalDateTime.now().minusMinutes(3));
        when(messageMapper.selectById(1L)).thenReturn(msg);

        ApiException ex = assertThrows(ApiException.class, () -> messageService.recallMessage(1L, 1L));
        assertEquals("只能撤回2分钟内的消息", ex.getMessage());
    }
}
