package dev.verrai.sample.databinding;

import dev.verrai.api.Bound;
import dev.verrai.api.DataField;
import dev.verrai.api.EventHandler;
import dev.verrai.api.Model;
import dev.verrai.api.RootElement;
import dev.verrai.api.Page;
import dev.verrai.api.Templated;
import dev.verrai.api.binding.BinderLifecycle;
import dev.verrai.api.binding.Subscription;
import jakarta.inject.Inject;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import dev.verrai.api.Navigation;

import java.util.ArrayList;
import java.util.List;

@Page(role = "databinding")
@Templated("DataBindingDemoPage.html")
public class DataBindingDemoPage implements BinderLifecycle {

    @RootElement
    public HTMLElement element;

    @Inject
    Navigation navigation;

    @Inject
    @Model
    public DemoModel model;

    @DataField
    @Bound
    public HTMLInputElement name;

    @DataField
    @Bound(property = "name")
    public HTMLInputElement nameOutput;

    @DataField
    @Bound(property = "address.street")
    public HTMLInputElement street;

    @DataField
    @Bound(property = "address.street")
    public HTMLInputElement streetOutput;

    @DataField
    public HTMLButtonElement backBtn;

    private List<Subscription> subscriptions = new ArrayList<>();

    @EventHandler(value = "backBtn", type = "click")
    public void onBackClicked() {
        navigation.goTo("dashboard");
    }

    @Override
    public void addBinding(Subscription s) {
        subscriptions.add(s);
    }

    @Override
    public void clearBindings() {
        for (Subscription s : subscriptions) {
            s.remove();
        }
        subscriptions.clear();
    }
}
