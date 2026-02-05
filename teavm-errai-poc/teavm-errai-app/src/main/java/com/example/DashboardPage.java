package com.example;

import com.example.errai.api.Page;
import com.example.errai.api.Templated;
import com.example.errai.api.DataField;
import com.example.errai.api.Navigation;
import com.example.errai.api.PageShowing;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.browser.Window;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Page(role = "dashboard")
@Templated
public class DashboardPage {

    @Inject
    public Navigation navigation;

    @Inject
    public HelloService service;

    public HTMLElement element;

    @DataField
    public HTMLButtonElement backBtn;

    @DataField
    public HTMLButtonElement userBtn;

    @DataField
    public HTMLElement content;

    @PageShowing
    public void onShow() {
        content.setInnerHTML("Welcome! " + service.getGreeting());
        backBtn.addEventListener("click", e -> navigation.goTo("login"));

        userBtn.addEventListener("click", e -> {
            Map<String, String> params = new HashMap<>();
            params.put("userId", "12345");
            params.put("name", "TeaVM User");
            navigation.goTo("user-profile", params);
        });
    }
}
