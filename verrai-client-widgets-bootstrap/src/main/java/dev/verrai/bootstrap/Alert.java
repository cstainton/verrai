package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;

public class Alert extends Widget {

    public enum Type {
        PRIMARY("alert-primary"),
        SUCCESS("alert-success"),
        DANGER("alert-danger"),
        WARNING("alert-warning"),
        INFO("alert-info");

        private final String cssClass;
        Type(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    private Type currentType = null;

    public Alert() {
        this.element = Window.current().getDocument().createElement("div");
        this.element.setClassName("alert");
        this.element.setAttribute("role", "alert");
    }

    public void setText(String text) {
        this.element.setInnerText(text);
    }

    public String getText() {
        return this.element.getInnerText();
    }

    public void setType(Type type) {
        this.currentType = type;
        this.element.setClassName("alert " + type.getCssClass());
    }

    public Type getType() {
        return currentType;
    }

    public void setDismissible(boolean dismissible) {
        // Remove existing close button if present
        NodeList<? extends Node> children = this.element.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child instanceof HTMLElement) {
                if ("true".equals(((HTMLElement) child).getAttribute("data-verrai-close"))) {
                    this.element.removeChild(child);
                }
            }
        }
        if (dismissible) {
            HTMLElement closeBtn =
                (HTMLElement) Window.current().getDocument().createElement("button");
            closeBtn.setClassName("btn-close");
            closeBtn.setAttribute("type", "button");
            closeBtn.setAttribute("aria-label", "Close");
            closeBtn.setAttribute("data-verrai-close", "true");
            closeBtn.addEventListener("click", e -> {
                Node parent = this.element.getParentNode();
                if (parent != null) {
                    parent.removeChild(this.element);
                }
            });
            this.element.appendChild(closeBtn);
        }
    }
}
