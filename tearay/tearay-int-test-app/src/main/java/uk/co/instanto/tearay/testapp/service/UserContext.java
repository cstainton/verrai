package uk.co.instanto.tearay.testapp.service;

import uk.co.instanto.tearay.api.cdi.SessionScoped;
import java.util.UUID;

@SessionScoped
public class UserContext {
    private String username;
    private boolean loggedIn;
    private String sessionId;

    public UserContext() {
        this.sessionId = UUID.randomUUID().toString();
    }

    public void set(String username) {
        this.username = username;
        this.loggedIn = true;
    }

    public void clear() {
        this.username = null;
        this.loggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getSessionId() {
        return sessionId;
    }
}
