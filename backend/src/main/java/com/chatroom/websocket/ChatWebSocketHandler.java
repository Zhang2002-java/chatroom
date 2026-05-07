package com.chatroom.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatroom.entity.GroupMember;
import com.chatroom.entity.Message;
import com.chatroom.entity.MessageRead;
import com.chatroom.mapper.GroupMemberMapper;
import com.chatroom.mapper.MessageMapper;
import com.chatroom.mapper.MessageReadMapper;
import com.chatroom.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final MessageMapper messageMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final MessageReadMapper messageReadMapper;

    public ChatWebSocketHandler(JwtTokenProvider jwtTokenProvider, MessageMapper messageMapper,
                                GroupMemberMapper groupMemberMapper, MessageReadMapper messageReadMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageMapper = messageMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.messageReadMapper = messageReadMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) { session.close(); return; }

        String query = uri.getQuery();
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    token = pair[1];
                }
            }
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        userSessions.put(userId, session);
        log.info("User {} connected via WebSocket", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        JsonNode json = objectMapper.readTree(textMessage.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "CHAT" -> handleChatMessage(json);
            case "RECALL" -> handleRecallMessage(json);
            case "STATUS" -> handleStatusMessage(json);
            case "READ_CONVERSATION" -> handleReadConversation(json);
            case "HEARTBEAT" -> sendMessage(session, "{\"type\":\"HEARTBEAT\"}");
        }
    }

    private void handleChatMessage(JsonNode json) throws IOException {
        Long senderId = json.get("senderId").asLong();
        Long receiverId = json.get("receiverId").asLong();
        String chatType = json.get("chatType").asText();
        String contentType = json.get("contentType").asText();
        String content = json.get("content").asText();

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setChatType(chatType);
        msg.setContentType(contentType);
        msg.setContent(content);
        msg.setStatus(1);
        msg.setIsRecalled(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        String responseJson = objectMapper.createObjectNode()
                .put("type", "CHAT")
                .put("messageId", msg.getId())
                .put("senderId", senderId)
                .put("receiverId", receiverId)
                .put("chatType", chatType)
                .put("contentType", contentType)
                .put("content", content)
                .put("status", 1)
                .put("timestamp", System.currentTimeMillis())
                .toString();

        if ("private".equals(chatType)) {
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                sendMessage(receiverSession, responseJson);
                msg.setStatus(2);
                messageMapper.updateById(msg);
                notifyStatusChange(senderId, msg.getId(), 2);
            }
            WebSocketSession senderSession = userSessions.get(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                sendMessage(senderSession, responseJson);
            }
        } else {
            // Group chat: broadcast to all online group members except sender
            LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(GroupMember::getGroupId, receiverId);
            java.util.List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
            for (GroupMember member : members) {
                if (member.getUserId().equals(senderId)) continue;
                WebSocketSession memberSession = userSessions.get(member.getUserId());
                if (memberSession != null && memberSession.isOpen()) {
                    sendMessage(memberSession, responseJson);
                }
            }
            // Echo to sender
            WebSocketSession senderSession = userSessions.get(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                sendMessage(senderSession, responseJson);
            }
        }
    }

    private void handleRecallMessage(JsonNode json) throws IOException {
        Long messageId = json.get("messageId").asLong();
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) return;

        msg.setIsRecalled(1);
        messageMapper.updateById(msg);

        ObjectNode recallJson = objectMapper.createObjectNode()
                .put("type", "RECALL")
                .put("messageId", messageId)
                .put("chatType", msg.getChatType())
                .put("receiverId", msg.getReceiverId())
                .put("senderId", msg.getSenderId());

        WebSocketSession receiverSession = userSessions.get(msg.getReceiverId());
        if (receiverSession != null && receiverSession.isOpen()) {
            sendMessage(receiverSession, recallJson.toString());
        }
        WebSocketSession senderSession = userSessions.get(msg.getSenderId());
        if (senderSession != null && senderSession.isOpen()) {
            sendMessage(senderSession, recallJson.toString());
        }
    }

    private void handleReadConversation(JsonNode json) throws IOException {
        Long targetId = json.get("targetId").asLong();
        String chatType = json.get("chatType").asText();
        Long currentUserId = json.get("currentUserId").asLong();

        if ("group".equals(chatType)) {
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getReceiverId, targetId)
                   .eq(Message::getChatType, "group")
                   .ne(Message::getSenderId, currentUserId);
            java.util.List<Message> messages = messageMapper.selectList(wrapper);
            for (Message msg : messages) {
                LambdaQueryWrapper<MessageRead> readWrapper = new LambdaQueryWrapper<>();
                readWrapper.eq(MessageRead::getMessageId, msg.getId())
                           .eq(MessageRead::getUserId, currentUserId);
                if (messageReadMapper.selectCount(readWrapper) == 0) {
                    MessageRead mr = new MessageRead();
                    mr.setMessageId(msg.getId());
                    mr.setUserId(currentUserId);
                    mr.setReadAt(LocalDateTime.now());
                    messageReadMapper.insert(mr);
                }
            }
            notifyGroupReadStatus(targetId, currentUserId);
        } else {
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getSenderId, targetId)
                   .eq(Message::getReceiverId, currentUserId)
                   .eq(Message::getChatType, chatType)
                   .lt(Message::getStatus, 3);

            java.util.List<Message> unreadMessages = messageMapper.selectList(wrapper);
            for (Message msg : unreadMessages) {
                msg.setStatus(3);
                messageMapper.updateById(msg);
                notifyStatusChange(msg.getSenderId(), msg.getId(), 3);
            }
        }
    }

    private void notifyGroupReadStatus(Long groupId, Long readerId) throws IOException {
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId);
        java.util.List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
        String notification = objectMapper.createObjectNode()
                .put("type", "GROUP_READ")
                .put("groupId", groupId)
                .put("readerId", readerId)
                .toString();
        for (GroupMember member : members) {
            WebSocketSession s = userSessions.get(member.getUserId());
            if (s != null && s.isOpen()) {
                sendMessage(s, notification);
            }
        }
    }

    private void handleStatusMessage(JsonNode json) throws IOException {
        Long messageId = json.get("messageId").asLong();
        int newStatus = json.get("status").asInt();

        Message msg = messageMapper.selectById(messageId);
        if (msg != null) {
            msg.setStatus(newStatus);
            messageMapper.updateById(msg);
            notifyStatusChange(msg.getSenderId(), messageId, newStatus);
        }
    }

    private void notifyStatusChange(Long userId, Long messageId, int status) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            String statusJson = objectMapper.createObjectNode()
                    .put("type", "STATUS")
                    .put("messageId", messageId)
                    .put("status", status)
                    .toString();
            sendMessage(session, statusJson);
        }
    }

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message));
        }
    }

    public void notifyUser(Long userId, ObjectNode notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, notification.toString());
            } catch (IOException e) {
                log.error("Failed to notify user {}", userId, e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.values().remove(session);
        log.info("WebSocket connection closed: {}", status);
    }
}
