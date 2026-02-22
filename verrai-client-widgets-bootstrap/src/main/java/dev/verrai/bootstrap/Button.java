package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import jakarta.enterprise.context.Dependent;

@Dependent
public class Button extends Widget {

    public enum Type {
        PRIMARY("btn-primary"),
        SUCCESS("btn-success"),
        DANGER("btn-danger"),
        WARNING("btn-warning"),
        INFO("btn-info");

        private final String cssClass;
        Type(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    public enum Size {
        SMALL("btn-sm"),
        NORMAL(""),
        LARGE("btn-lg");

        private final String cssClass;
        Size(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    private Type currentType = null;
    private Size currentSize = Size.NORMAL;
    private boolean outline = false;

    public Button() {
        this.element = Window.current().getDocument().createElement("button");
        this.element.setClassName("btn");
    }

    public void setText(String text) {
        this.element.setInnerText(text);
    }

    public String getText() {
        return this.element.getInnerText();
    }

    public void setType(Type type) {
        this.currentType = type;
        updateClass();
    }

    public void setSize(Size size) {
        this.currentSize = size;
        updateClass();
    }

    public void setOutline(boolean outline) {
        this.outline = outline;
        updateClass();
    }

    public void setDisabled(boolean disabled) {
        if (disabled) {
            this.element.setAttribute("disabled", "true");
        } else {
            this.element.removeAttribute("disabled");
        }
    }

    public boolean isDisabled() {
        return "true".equals(this.element.getAttribute("disabled"))
            || this.element.getAttribute("disabled") != null
            && this.element.getAttribute("disabled").isEmpty();
    }

    public void addClickListener(org.teavm.jso.dom.events.EventListener<?> listener) {
        this.element.addEventListener("click", listener);
    }

    private void updateClass() {
        StringBuilder sb = new StringBuilder("btn");
        if (currentType != null) {
            String base = currentType.getCssClass(); // e.g. "btn-primary"
            if (outline) {
                // Convert "btn-primary" → "btn-outline-primary"
                base = base.replace("btn-", "btn-outline-");
            }
            sb.append(" ").append(base);
        }
        if (currentSize != Size.NORMAL) {
            sb.append(" ").append(currentSize.getCssClass());
        }
        this.element.setClassName(sb.toString());
    }
}
