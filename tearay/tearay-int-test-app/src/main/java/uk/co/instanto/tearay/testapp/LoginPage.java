package uk.co.instanto.tearay.testapp;

import uk.co.instanto.tearay.api.Page;
import uk.co.instanto.tearay.api.Templated;
import uk.co.instanto.tearay.api.DataField;
import uk.co.instanto.tearay.api.Navigation;
import uk.co.instanto.tearay.api.PageShowing;
import uk.co.instanto.tearay.api.EventHandler;
import uk.co.instanto.tearay.testapp.service.AuthService;
import uk.co.instanto.tearay.testapp.service.UserContext;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.browser.Window;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Page(role = "login")
@Templated
public class LoginPage {

    @Inject
    public Navigation navigation;

    @Inject
    public AuthService authService;

    @Inject
    public UserContext userContext;

    public HTMLElement element;

    @DataField
    public HTMLButtonElement loginBtn;

    @DataField
    public HTMLButtonElement adminLoginBtn;

    @EventHandler("loginBtn")
    public void onLoginClick() {
        // Hardcoded "test" user login
        if (authService.login("test", "password")) {
            userContext.set("test");
            Map<String, String> params = new HashMap<>();
            params.put("username", "test");
            navigation.goTo("dashboard", params);
        } else {
            Window.alert("Login Failed");
        }
    }

    @EventHandler("adminLoginBtn")
    public void onAdminLoginClick() {
        // Simulate failure for non-test user
        if (authService.login("admin", "password")) {
             // Should fail
             userContext.set("admin");
             navigation.goTo("dashboard");
        } else {
             Window.alert("Login Failed: User 'admin' not allowed");
        }
    }
}
