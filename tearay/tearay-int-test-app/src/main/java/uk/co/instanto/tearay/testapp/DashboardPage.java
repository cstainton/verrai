package uk.co.instanto.tearay.testapp;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.browser.Window;
import uk.co.instanto.tearay.widgets.*;
import uk.co.instanto.tearay.api.*;
import uk.co.instanto.tearay.api.cdi.*;
import uk.co.instanto.tearay.testapp.service.UserContext;
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

    @Inject
    public UserContext userContext;

    @PageState
    public String username;

    public HTMLElement element;

    @Inject @DataField
    public Container container;

    @PageShowing
    public void onShow() {
        // Check session
        if (!userContext.isLoggedIn()) {
            navigation.goTo("login");
            return;
        }

        // Clear previous content if re-used
        container.element.setInnerHTML("");

        // Navbar
        Navbar navbar = new Navbar();
        navbar.setBrand("Tearay App");
        navbar.setSticky(true);
        navbar.addLink("Home", e -> Window.alert("Home clicked"));
        navbar.addLink("Profile", e -> {
            Map<String, String> params = new HashMap<>();
            params.put("userId", "12345");
            navigation.goTo("user-profile", params);
        });
        container.element.appendChild(navbar.element);

        Row row = new Row();
        container.element.appendChild(row.element);

        // Col 1: Card
        Column col1 = new Column().span(6);
        row.element.appendChild(col1.element);

        Card card = new Card();
        card.setTitle("Welcome " + userContext.getUsername());
        card.setText(service.getGreeting());
        col1.element.appendChild(card.element);

        Button profileBtn = new Button();
        profileBtn.setText("Go to User Profile");
        profileBtn.setType(Button.Type.PRIMARY);
        profileBtn.addClickListener(e -> {
            Map<String, String> params = new HashMap<>();
            params.put("userId", "12345");
            params.put("name", "TeaVM User");
            navigation.goTo("user-profile", params);
        });
        card.addContent(profileBtn);

        // Col 2: Alert & Logout
        Column col2 = new Column().span(6);
        row.element.appendChild(col2.element);

        Alert alert = new Alert();
        alert.setType(Alert.Type.INFO);
        alert.setText("Session Active for: " + userContext.getUsername());
        col2.element.appendChild(alert.element);

        Button logoutBtn = new Button();
        logoutBtn.setText("Logout");
        logoutBtn.setType(Button.Type.DANGER);
        logoutBtn.addClickListener(e -> {
            userContext.clear();
            navigation.goTo("login");
        });
        col2.element.appendChild(logoutBtn.element);
    }
}
