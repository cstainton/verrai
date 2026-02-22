package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;
import dev.verrai.api.IsWidget;

public abstract class Widget implements IsWidget {
    public HTMLElement element;

    @Override
    public HTMLElement getElement() {
        return element;
    }

    /**
     * Shows an inline validation error message next to this widget.
     * Adds the Bootstrap {@code is-invalid} class to the element and inserts
     * a sibling {@code <div class="invalid-feedback">} after it.
     */
    public void setErrorMessage(String message) {
        if (element == null) return;
        // Add is-invalid class if not already present
        String cls = element.getClassName();
        if (!cls.contains("is-invalid")) {
            element.setClassName(cls.isEmpty() ? "is-invalid" : cls + " is-invalid");
        }
        // Remove any previous feedback sibling, then insert the new one
        removeFeedbackSibling();
        Node parent = element.getParentNode();
        if (parent != null) {
            HTMLElement feedback = (HTMLElement) Window.current().getDocument().createElement("div");
            feedback.setClassName("invalid-feedback");
            feedback.setAttribute("data-verrai-feedback", "true");
            feedback.setInnerText(message);
            parent.insertBefore(feedback, element.getNextSibling());
        }
    }

    /**
     * Removes any inline validation error message and restores the element's
     * normal appearance by removing the {@code is-invalid} CSS class.
     */
    public void clearErrorMessage() {
        if (element == null) return;
        String cls = element.getClassName();
        element.setClassName(cls.replace(" is-invalid", "").replace("is-invalid", "").trim());
        removeFeedbackSibling();
    }

    private void removeFeedbackSibling() {
        Node parent = element.getParentNode();
        if (parent == null) return;
        NodeList<? extends Node> children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof HTMLElement) {
                if ("true".equals(((HTMLElement) child).getAttribute("data-verrai-feedback"))) {
                    parent.removeChild(child);
                    return;
                }
            }
        }
    }
}
