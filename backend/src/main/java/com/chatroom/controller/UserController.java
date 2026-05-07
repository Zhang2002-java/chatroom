package com.chatroom.controller;

import com.chatroom.common.Result;
import com.chatroom.entity.User;
import com.chatroom.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        return Result.ok(userService.searchUsers(keyword));
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @PutMapping("/me")
    public Result<?> updateProfile(@RequestBody Map<String, String> body) {
        userService.updateProfile(getCurrentUserId(), body.get("nickname"), body.get("signature"));
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
