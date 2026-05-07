package com.chatroom.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdGluZy1wdXJwb3Nlcy0yNTYtYml0cw==",
            3600000L
        );
    }

    @Test
    void generateToken_producesNonEmptyString() {
        String token = jwtTokenProvider.generateToken(1L, "testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void getUserIdFromToken_returnsCorrectId() {
        String token = jwtTokenProvider.generateToken(42L, "user");
        assertEquals(42L, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void validateToken_returnsTrue_forValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "test");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_returnsFalse_forTamperedToken() {
        String token = jwtTokenProvider.generateToken(1L, "test");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    void validateToken_returnsFalse_forExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdGluZy1wdXJwb3Nlcy0yNTYtYml0cw==",
            1L
        );
        String token = shortLived.generateToken(1L, "test");
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void validateToken_returnsFalse_forNullToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
