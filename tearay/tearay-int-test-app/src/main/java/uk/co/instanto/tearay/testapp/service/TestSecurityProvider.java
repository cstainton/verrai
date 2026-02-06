package uk.co.instanto.tearay.testapp.service;

import uk.co.instanto.tearay.api.cdi.ApplicationScoped;
import uk.co.instanto.tearay.api.SecurityProvider;
import javax.inject.Inject;

@ApplicationScoped
public class TestSecurityProvider implements SecurityProvider {

    @Inject
    public UserContext userContext;

    @Override
    public boolean hasRole(String role) {
        if (!userContext.isLoggedIn()) {
            return false;
        }
        // Simple simulation: logged in user 'test' has all roles
        return "test".equals(userContext.getUsername());
    }
}
