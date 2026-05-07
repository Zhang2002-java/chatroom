package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.service.FriendService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public Result<?> sendRequest(@RequestParam Long friendId) {
        friendService.sendRequest(getCurrentUserId(), friendId);
        return Result.ok();
    }

    @PutMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        friendService.acceptRequest(id, getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id) {
        friendService.rejectRequest(id, getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        friendService.deleteFriend(id, getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/block")
    public Result<?> block(@PathVariable Long id) {
        friendService.blockFriend(id, getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/unblock")
    public Result<?> unblock(@PathVariable Long id) {
        friendService.unblockFriend(id, getCurrentUserId());
        return Result.ok();
    }

    @GetMapping
    public Result<Map<String, Object>> getFriends() {
        Long userId = getCurrentUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("friends", friendService.getFriendList(userId));
        data.put("pending", friendService.getPendingRequests(userId));
        data.put("blocked", friendService.getBlockedList(userId));
        return Result.ok(data);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
