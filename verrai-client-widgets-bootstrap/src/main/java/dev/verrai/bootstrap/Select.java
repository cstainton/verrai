package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLSelectElement;
import org.teavm.jso.dom.html.HTMLOptionElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;
import org.teavm.jso.dom.events.EventListener;
import dev.verrai.api.TakesValue;

public class Select extends Widget implements TakesValue<String> {

    public enum Size {
        DEFAULT(""),
        SMALL("form-select-sm"),
        LARGE("form-select-lg");

        private final String cssClass;
        Size(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    private HTMLSelectElement select;
    private Size size = Size.DEFAULT;

    public Select() {
        this.element = Window.current().getDocument().createElement("select");
        this.select = (HTMLSelectElement) this.element;
        updateClass();
    }

    public void addOption(String text, String value) {
        HTMLOptionElement option =
            (HTMLOptionElement) Window.current().getDocument().createElement("option");
        option.setInnerText(text);
        option.setValue(value);
        this.select.appendChild(option);
    }

    public void clearOptions() {
        while (select.getFirstChild() != null) {
            select.removeChild(select.getFirstChild());
        }
    }

    public void removeOption(String value) {
        NodeList<? extends Node> nodes = select.getChildNodes();
        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node n = nodes.item(i);
            if (n instanceof HTMLOptionElement) {
                HTMLOptionElement opt = (HTMLOptionElement) n;
                if (value.equals(opt.getValue())) {
                    select.removeChild(n);
                    break;
                }
            }
        }
    }

    public void setSize(Size size) {
        this.size = size;
        updateClass();
    }

    public void setDisabled(boolean disabled) {
        this.select.setDisabled(disabled);
    }

    public boolean isDisabled() {
        return this.select.isDisabled();
    }

    public String getSelectedValue() {
        return this.select.getValue();
    }

    public void addChangeListener(EventListener<?> listener) {
        this.element.addEventListener("change", listener);
    }

    @Override
    public void setValue(String value) {
        this.select.setValue(value);
    }

    @Override
    public String getValue() {
        return this.select.getValue();
    }

    private void updateClass() {
        String base = "form-select";
        if (size != Size.DEFAULT) {
            base += " " + size.getCssClass();
        }
        this.element.setClassName(base);
    }
}
