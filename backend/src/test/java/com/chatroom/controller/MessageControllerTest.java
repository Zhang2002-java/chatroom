package com.chatroom.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageControllerTest extends BaseControllerTest {

    @BeforeEach
    void setupUsers() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"pass123\"}"));
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"pass123\"}"));
        setCurrentUser(1L);
    }

    @Test
    void getMessages_private_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/messages")
                .param("targetId", "2")
                .param("chatType", "private")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getMessages_group_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/messages")
                .param("targetId", "1")
                .param("chatType", "group")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void searchMessages_returnsFilteredList() throws Exception {
        mockMvc.perform(post("/api/v1/messages/search")
                .param("keyword", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void recallMessage_notFound_returnsError() throws Exception {
        mockMvc.perform(put("/api/v1/messages/999/recall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("消息不存在"));
    }
}
