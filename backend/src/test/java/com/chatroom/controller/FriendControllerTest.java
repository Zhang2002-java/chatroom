package com.chatroom.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class FriendControllerTest extends BaseControllerTest {

    @BeforeEach
    void setupUsers() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"pass123\"}"));
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"pass123\"}"));
        setCurrentUser(1L);
    }

    @Test
    void sendRequest_success() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request")
                .param("friendId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void sendRequest_self_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request")
                .param("friendId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能添加自己为好友"));
    }

    @Test
    void acceptRequest_success() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request").param("friendId", "2"));
        setCurrentUser(2L);

        mockMvc.perform(put("/api/v1/friends/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void rejectRequest_success() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request").param("friendId", "2"));
        setCurrentUser(2L);

        mockMvc.perform(put("/api/v1/friends/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteFriend_success() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request").param("friendId", "2"));
        setCurrentUser(2L);
        mockMvc.perform(put("/api/v1/friends/1/accept"));

        mockMvc.perform(delete("/api/v1/friends/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getFriends_returnsFriendsAndPending() throws Exception {
        mockMvc.perform(post("/api/v1/friends/request").param("friendId", "2"));
        setCurrentUser(2L);
        mockMvc.perform(put("/api/v1/friends/1/accept"));

        setCurrentUser(1L);
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.friends").isArray())
                .andExpect(jsonPath("$.data.pending").isArray());
    }
}
