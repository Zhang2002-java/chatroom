package com.chatroom.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupControllerTest extends BaseControllerTest {

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
    void createGroup_returnsGroupInfo() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                .param("name", "TestGroup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("TestGroup"))
                .andExpect(jsonPath("$.data.ownerId").value(1));
    }

    @Test
    void getMyGroups_returnsList() throws Exception {
        mockMvc.perform(post("/api/v1/groups").param("name", "MyGroup"));

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("MyGroup"));
    }

    @Test
    void getMembers_returnsMemberList() throws Exception {
        mockMvc.perform(post("/api/v1/groups").param("name", "Group"));

        mockMvc.perform(get("/api/v1/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].role").value("owner"));
    }

    @Test
    void addMember_success() throws Exception {
        mockMvc.perform(post("/api/v1/groups").param("name", "Group"));

        mockMvc.perform(post("/api/v1/groups/1/members")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void removeMember_success() throws Exception {
        mockMvc.perform(post("/api/v1/groups").param("name", "Group"));
        mockMvc.perform(post("/api/v1/groups/1/members").param("userId", "2"));
        setCurrentUser(2L);

        mockMvc.perform(delete("/api/v1/groups/1/members/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
