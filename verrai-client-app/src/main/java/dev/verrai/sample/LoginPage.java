package dev.verrai.sample;

import dev.verrai.api.Page;
import dev.verrai.api.Templated;
import dev.verrai.api.DataField;
import dev.verrai.api.Navigation;
import dev.verrai.api.PageShowing;
import dev.verrai.api.RootElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Page(role = "login", startingPage = true)
@Templated
public class LoginPage {

    @Inject
    public Navigation navigation;

    @Inject
    public AppSecurityProvider securityProvider;

    @RootElement
    public HTMLElement element;

    @DataField
    public HTMLButtonElement loginBtn;

    @DataField
    public HTMLButtonElement adminLoginBtn;

    private org.teavm.jso.workers.Worker worker;
    private HTMLElement debugOverlay;

    @PageShowing
    public void onShow() {
        initWorker();

        loginBtn.addEventListener("click", e -> {
            securityProvider.setRoles("user");

            Map<String, String> params = new HashMap<>();
            params.put("username", "RegularUser");
            navigation.goTo("dashboard", params);
        });

        adminLoginBtn.addEventListener("click", e -> {
            securityProvider.setRoles("admin");
            Map<String, String> params = new HashMap<>();
            params.put("username", "Administrator");
            navigation.goTo("dashboard", params);
        });
    }

    private void initWorker() {
        // Worker removed
    }
}
