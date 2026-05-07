package com.chatroom.websocket;

import com.chatroom.entity.Message;
import com.chatroom.mapper.GroupMemberMapper;
import com.chatroom.mapper.MessageMapper;
import com.chatroom.mapper.MessageReadMapper;
import com.chatroom.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private MessageMapper messageMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private MessageReadMapper messageReadMapper;
    @Mock private WebSocketSession session;

    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(jwtTokenProvider, messageMapper, groupMemberMapper, messageReadMapper);
    }

    @Test
    void afterConnectionEstablished_withValidToken_storesSession() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=valid-token"));
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(1L);

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablished_withInvalidToken_closesSession() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=bad-token"));
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void afterConnectionEstablished_withNullUri_closesSession() throws Exception {
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    @Test
    void afterConnectionEstablished_withNoToken_closesSession() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws"));
        when(jwtTokenProvider.validateToken(null)).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void handleHeartbeat_respondsWithHeartbeat() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=valid"));
        when(jwtTokenProvider.validateToken("valid")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid")).thenReturn(1L);
        handler.afterConnectionEstablished(session);

        TextMessage heartbeat = new TextMessage("{\"type\":\"HEARTBEAT\"}");
        handler.handleTextMessage(session, heartbeat);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=valid"));
        when(jwtTokenProvider.validateToken("valid")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid")).thenReturn(1L);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        handler.notifyUser(1L, new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void notifyUser_doesNothingIfUserNotConnected() throws IOException {
        handler.notifyUser(99L, new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
