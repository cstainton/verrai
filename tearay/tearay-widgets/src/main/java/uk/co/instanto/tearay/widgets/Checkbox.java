package uk.co.instanto.tearay.widgets;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

public class Checkbox extends Widget {

    private HTMLInputElement input;
    private HTMLElement label;

    public Checkbox(String labelText) {
        this.element = Window.current().getDocument().createElement("div");
        this.element.setClassName("form-check");

        input = (HTMLInputElement) Window.current().getDocument().createElement("input");
        input.setClassName("form-check-input");
        input.setType("checkbox");
        this.element.appendChild(input);

        label = Window.current().getDocument().createElement("label");
        label.setClassName("form-check-label");
        label.setInnerText(labelText);
        this.element.appendChild(label);
    }

    public void setValue(boolean checked) {
        input.setChecked(checked);
    }

    public boolean getValue() {
        return input.isChecked();
    }

    public void addChangeHandler(org.teavm.jso.dom.events.EventListener<?> listener) {
        input.addEventListener("change", listener);
    }
}
