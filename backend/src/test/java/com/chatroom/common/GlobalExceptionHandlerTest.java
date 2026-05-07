package com.chatroom.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiException_returnsResultWithCodeAndMessage() {
        ApiException ex = new ApiException("测试错误");
        Result<?> result = handler.handleApiException(ex);

        assertEquals(400, result.getCode());
        assertEquals("测试错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void handleApiException_withCustomCode() {
        ApiException ex = new ApiException(403, "无权限");
        Result<?> result = handler.handleApiException(ex);

        assertEquals(403, result.getCode());
        assertEquals("无权限", result.getMessage());
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("数据库连接失败");
        Result<?> result = handler.handleException(ex);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("数据库连接失败"));
        assertTrue(result.getMessage().startsWith("服务器内部错误"));
    }
}
