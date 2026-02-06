package uk.co.instanto.tearay.testapp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import uk.co.instanto.tearay.testapp.service.AuthService;
import uk.co.instanto.tearay.testapp.service.AuthService_Factory;
import uk.co.instanto.tearay.testapp.service.TestSecurityProvider;
import uk.co.instanto.tearay.testapp.service.TestSecurityProvider_Factory;
import uk.co.instanto.tearay.testapp.service.UserContext;
import uk.co.instanto.tearay.api.impl.SessionContext;
import static org.junit.Assert.*;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class AuthTest {

    @Test
    public void testAuthenticationFlow() {
        // Clear session first
        SessionContext.getInstance().clear();

        // 1. Auth Service
        AuthService auth = AuthService_Factory.getInstance();
        assertFalse("Wrong credentials should fail", auth.login("user", "wrong"));
        assertTrue("Correct credentials should pass", auth.login("test", "password"));

        // 2. Security Provider & User Context (Mocking the flow of LoginPage)
        TestSecurityProvider security = TestSecurityProvider_Factory.getInstance();
        UserContext context = security.userContext; // Injected

        assertNotNull(context);
        assertFalse("Initially not logged in", context.isLoggedIn());
        assertFalse("Security should deny role", security.hasRole("admin"));

        // 3. Simulate Login
        context.set("test");
        assertTrue("Context should be logged in", context.isLoggedIn());
        assertEquals("test", context.getUsername());

        // 4. Check Security
        assertTrue("Security should grant role to 'test' user", security.hasRole("admin"));

        // 5. Logout
        context.clear();
        assertFalse("Context should be cleared", context.isLoggedIn());
        assertFalse("Security should deny role after logout", security.hasRole("admin"));
    }
}
