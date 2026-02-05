package uk.co.instanto.tearay.sample;

import uk.co.instanto.tearay.api.Page;
import uk.co.instanto.tearay.api.Templated;
import uk.co.instanto.tearay.api.DataField;
import uk.co.instanto.tearay.api.Navigation;
import uk.co.instanto.tearay.api.PageShowing;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.browser.Window;
import javax.inject.Inject;

@Page(role = "login")
@Templated
public class LoginPage {

    @Inject
    public Navigation navigation;

    @Inject
    public AppSecurityProvider securityProvider;

    public HTMLElement element;

    @DataField
    public HTMLButtonElement loginBtn;

    @DataField
    public HTMLButtonElement adminLoginBtn;

    @PageShowing
    public void onShow() {
        loginBtn.addEventListener("click", e -> {
            securityProvider.setRoles("user");
            navigation.goTo("dashboard");
        });

        adminLoginBtn.addEventListener("click", e -> {
            securityProvider.setRoles("admin");
            navigation.goTo("dashboard");
        });
    }
}
