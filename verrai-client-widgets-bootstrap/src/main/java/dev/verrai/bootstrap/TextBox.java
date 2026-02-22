package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.events.EventListener;
import dev.verrai.api.TakesValue;

public class TextBox extends Widget implements TakesValue<String> {

    private HTMLInputElement input;

    public TextBox() {
        this.element = Window.current().getDocument().createElement("input");
        this.input = (HTMLInputElement) this.element;
        this.input.setType("text");
        this.element.setClassName("form-control");
    }

    @Override
    public void setValue(String value) {
        input.setValue(value != null ? value : "");
    }

    @Override
    public String getValue() {
        return input.getValue();
    }

    public String getText() {
        return input.getValue();
    }

    public void setPlaceholder(String text) {
        input.setAttribute("placeholder", text);
    }

    public String getPlaceholder() {
        return input.getAttribute("placeholder");
    }

    public void setReadOnly(boolean readOnly) {
        input.setReadOnly(readOnly);
    }

    public boolean isReadOnly() {
        return input.isReadOnly();
    }

    public void setDisabled(boolean disabled) {
        input.setDisabled(disabled);
    }

    public boolean isDisabled() {
        return input.isDisabled();
    }

    /** Fires on every keystroke (the DOM {@code input} event). */
    public void addInputListener(EventListener<?> listener) {
        this.element.addEventListener("input", listener);
    }

    public void addChangeHandler(EventListener<?> listener) {
        this.element.addEventListener("change", listener);
    }
}
