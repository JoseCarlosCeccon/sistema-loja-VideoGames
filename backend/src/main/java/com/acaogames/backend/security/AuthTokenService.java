package com.acaogames.backend.security;

import com.acaogames.backend.model.UsuarioSistema;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    private static final Duration SESSION_DURATION = Duration.ofHours(12);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionUser> sessions = new ConcurrentHashMap<String, SessionUser>();

    public String issueToken(UsuarioSistema usuario) {
        cleanupExpiredSessions();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        sessions.put(token, new SessionUser(usuario, Instant.now().plus(SESSION_DURATION)));
        return token;
    }

    public Optional<UsuarioSistema> findUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        SessionUser sessionUser = sessions.get(token);
        if (sessionUser == null) {
            return Optional.empty();
        }

        if (sessionUser.isExpired()) {
            sessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(sessionUser.getUsuario());
    }

    private void cleanupExpiredSessions() {
        Iterator<Map.Entry<String, SessionUser>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionUser> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    private static class SessionUser {
        private final UsuarioSistema usuario;
        private final Instant expiresAt;

        SessionUser(UsuarioSistema usuario, Instant expiresAt) {
            this.usuario = usuario;
            this.expiresAt = expiresAt;
        }

        UsuarioSistema getUsuario() {
            return usuario;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
