package com.chatroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatroom.common.Result;
import com.chatroom.entity.Message;
import com.chatroom.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public Result<Page<Message>> getMessages(
            @RequestParam Long targetId,
            @RequestParam String chatType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = getCurrentUserId();
        return Result.ok(messageService.getMessages(targetId, chatType, page, size, currentUserId));
    }

    @PostMapping("/search")
    public Result<List<Message>> searchMessages(@RequestParam String keyword) {
        return Result.ok(messageService.searchMessages(keyword, getCurrentUserId()));
    }

    @PutMapping("/{id}/recall")
    public Result<?> recallMessage(@PathVariable Long id) {
        messageService.recallMessage(id, getCurrentUserId());
        return Result.ok();
    }

    @GetMapping("/{id}/reads")
    public Result<List<Map<String, Object>>> getReadUsers(@PathVariable Long id) {
        return Result.ok(messageService.getReadUsers(id));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
