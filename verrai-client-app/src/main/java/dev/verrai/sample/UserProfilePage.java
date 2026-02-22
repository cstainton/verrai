package dev.verrai.sample;

import dev.verrai.api.Page;
import dev.verrai.api.Templated;
import dev.verrai.api.DataField;
import dev.verrai.api.Navigation;
import dev.verrai.api.PageShowing;
import dev.verrai.api.PageState;
import dev.verrai.security.RestrictedAccess;
import dev.verrai.api.RootElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import jakarta.inject.Inject;

@Page(role = "user-profile")
@RestrictedAccess(roles = { "admin", "user" })
@Templated
public class UserProfilePage {

    @Inject
    public Navigation navigation;

    @RootElement
    public HTMLElement element;

    @PageState
    public String userId;

    @PageState
    public String name;

    @DataField
    public HTMLElement idSpan;

    @DataField
    public HTMLElement nameSpan;

    @DataField
    public HTMLButtonElement backBtn;

    @DataField
    @RestrictedAccess(roles = "admin")
    public HTMLButtonElement adminBtn;

    @PageShowing
    public void onShow() {
        idSpan.setInnerText(userId != null ? userId : "N/A");
        nameSpan.setInnerText(name != null ? name : "N/A");

        backBtn.addEventListener("click", e -> navigation.goTo("dashboard"));
        adminBtn.addEventListener("click", e -> org.teavm.jso.browser.Window.alert("Admin Action Executed!"));
    }
}
