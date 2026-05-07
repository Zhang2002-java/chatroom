package com.chatroom.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadControllerTest extends BaseControllerTest {

    @BeforeEach
    void setupUsers() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\":\"uploader\",\"password\":\"pass123\"}"));
        setCurrentUser(1L);
    }

    @Test
    void upload_returnsUrlAndFileName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").isString())
                .andExpect(jsonPath("$.data.fileName").value("test.png"));
    }
}
