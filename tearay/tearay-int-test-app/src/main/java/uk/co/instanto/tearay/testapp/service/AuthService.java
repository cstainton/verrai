package uk.co.instanto.tearay.testapp.service;

import uk.co.instanto.tearay.api.cdi.ApplicationScoped;

@ApplicationScoped
public class AuthService {

    public boolean login(String username, String password) {
        return "test".equals(username);
    }
}
