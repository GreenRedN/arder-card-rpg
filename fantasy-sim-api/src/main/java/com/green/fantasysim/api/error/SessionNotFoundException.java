package com.green.fantasysim.api.error;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String sessionId) {
        super("session not found: " + sessionId);
    }
}
