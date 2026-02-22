package dev.verrai.sample;

import dev.verrai.ui.Composite;
import dev.verrai.api.DataField;
import dev.verrai.api.Templated;
import jakarta.enterprise.context.Dependent;
import jakarta.annotation.PostConstruct;
import org.teavm.jso.dom.html.HTMLElement;
import jakarta.inject.Inject;

@Dependent
@Templated
public class UserCard extends Composite {

    @DataField
    public HTMLElement nameSpan;

    @DataField
    public HTMLElement actionButton;

    @PostConstruct
    public void init() {
        actionButton.addEventListener("click", e -> {
            nameSpan.setInnerText("Clicked!");
        });
    }

    public void setName(String name) {
        nameSpan.setInnerText(name);
    }
}
