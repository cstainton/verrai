package uk.co.instanto.tearay.testapp.service;

import uk.co.instanto.tearay.api.cdi.SessionScoped;

@SessionScoped
public class UserContext {
    private String username;
    private boolean loggedIn;

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
}
