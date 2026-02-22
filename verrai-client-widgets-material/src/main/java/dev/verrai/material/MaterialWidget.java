package dev.verrai.material;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;
import dev.verrai.api.IsWidget;

public abstract class MaterialWidget implements IsWidget {
    public HTMLElement element;

    @Override
    public HTMLElement getElement() {
        return element;
    }

    /**
     * Shows an inline validation error message using the MDC helper-text pattern.
     * Adds {@code mdc-text-field--invalid} to the root element and inserts an
     * MDC helper-line sibling with validation styling.
     */
    public void setErrorMessage(String message) {
        if (element == null) return;
        String cls = element.getClassName();
        if (!cls.contains("mdc-text-field--invalid")) {
            element.setClassName(cls + " mdc-text-field--invalid");
        }
        removeHelperTextSibling();
        Node parent = element.getParentNode();
        if (parent != null) {
            HTMLElement helperLine = (HTMLElement) Window.current().getDocument().createElement("div");
            helperLine.setClassName("mdc-text-field-helper-line");
            helperLine.setAttribute("data-verrai-feedback", "true");

            HTMLElement helperText = (HTMLElement) Window.current().getDocument().createElement("div");
            helperText.setClassName(
                "mdc-text-field-helper-text mdc-text-field-helper-text--validation-msg");
            helperText.setInnerText(message);

            helperLine.appendChild(helperText);
            parent.insertBefore(helperLine, element.getNextSibling());
        }
    }

    /**
     * Removes any inline validation error message and restores the element's
     * normal appearance by removing the {@code mdc-text-field--invalid} class.
     */
    public void clearErrorMessage() {
        if (element == null) return;
        String cls = element.getClassName();
        element.setClassName(
            cls.replace(" mdc-text-field--invalid", "")
               .replace("mdc-text-field--invalid", "")
               .trim());
        removeHelperTextSibling();
    }

    private void removeHelperTextSibling() {
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
