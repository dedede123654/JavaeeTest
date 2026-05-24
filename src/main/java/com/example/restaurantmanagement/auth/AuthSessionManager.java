package com.example.restaurantmanagement.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionManager {

    public static final String TOKEN_HEADER = "X-Auth-Token";
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final ConcurrentMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public String createSession(Integer userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionInfo(userId, LocalDateTime.now().plus(SESSION_TTL)));
        return token;
    }

    public SessionInfo getSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalizedToken = token.trim();
        SessionInfo session = sessions.get(normalizedToken);
        if (session == null) {
            return null;
        }
        if (session.expireAt().isBefore(LocalDateTime.now())) {
            sessions.remove(normalizedToken);
            return null;
        }
        return session;
    }

    public record SessionInfo(Integer userId, LocalDateTime expireAt) {
    }
}
