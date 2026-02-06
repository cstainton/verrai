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

        String sessionId = context.getSessionId();
        assertNotNull(sessionId);

        // 3. Simulate Login
        context.set("test");
        assertTrue("Context should be logged in", context.isLoggedIn());
        assertEquals("test", context.getUsername());
        assertEquals("Session ID should persist during login", sessionId, context.getSessionId());

        // 4. Check Security
        assertTrue("Security should grant role to 'test' user", security.hasRole("admin"));

        // 5. Logout (Clear Session)
        SessionContext.getInstance().clear();

        // After clearing session, we need to re-fetch the bean to get the NEW session
        // Note: The 'security' bean is ApplicationScoped, so it still holds the OLD 'context' reference
        // if it was injected once?
        // Wait, IOCProcessor injects once. If 'security' is Singleton, it holds one 'userContext'.
        // If 'UserContext' is SessionScoped, the Factory should delegate to SessionContext.
        // BUT, the injection in 'security' happened at creation:
        // bean.userContext = UserContext_Factory.getInstance();
        // UserContext_Factory.getInstance() returns the OBJECT from SessionContext.
        // So 'security.userContext' points to the OLD object.
        // Clearing SessionContext removes it from the map, but 'security' still holds the old reference.
        // This is a known issue with Singletons injecting Session beans without Proxies.
        // Errai/CDI solves this with Proxies. We don't have proxies.
        // Therefore, we must re-fetch UserContext manually to test the new session ID.

        UserContext newContext = uk.co.instanto.tearay.testapp.service.UserContext_Factory.getInstance();
        assertNotSame("New context should be different object", context, newContext);
        assertNotEquals("New context should have different session ID", sessionId, newContext.getSessionId());
    }
}
