package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.entity.GroupInfo;
import com.chatroom.service.GroupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public Result<GroupInfo> create(@RequestParam String name) {
        return Result.ok(groupService.createGroup(name, getCurrentUserId()));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getMyGroups() {
        return Result.ok(groupService.getMyGroups(getCurrentUserId()));
    }

    @GetMapping("/{id}/members")
    public Result<List<Map<String, Object>>> getMembers(@PathVariable Long id) {
        return Result.ok(groupService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public Result<?> addMember(@PathVariable Long id, @RequestParam Long userId) {
        groupService.addMember(id, userId, getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<?> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId, getCurrentUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/invite")
    public Result<?> inviteFriends(@PathVariable Long id, @RequestBody List<Long> friendIds) {
        return Result.ok(groupService.inviteFriends(id, friendIds, getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id, getCurrentUserId());
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
