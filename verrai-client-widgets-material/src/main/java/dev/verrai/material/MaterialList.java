package dev.verrai.material;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.events.EventListener;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design list widget (MDC List pattern).
 * Supports optional leading icons and click listeners per item.
 */
@Dependent
public class MaterialList extends MaterialWidget {

    private final HTMLElement list;

    public MaterialList() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-list-group");

        list = (HTMLElement) Window.current().getDocument().createElement("ul");
        list.setClassName("mdc-list");
        list.setAttribute("role", "listbox");
        this.element.appendChild(list);
    }

    /**
     * Appends a list item with optional icon and click handler.
     *
     * @param text      display text
     * @param iconName  Material icon name (e.g. {@code "inbox"}), or {@code null} to omit
     * @param listener  click listener, or {@code null}
     */
    public void addItem(String text, String iconName, EventListener<?> listener) {
        HTMLElement item = (HTMLElement) Window.current().getDocument().createElement("li");
        item.setClassName("mdc-list-item");
        item.setAttribute("role", "option");
        item.setAttribute("tabindex", "0");

        HTMLElement ripple = (HTMLElement) Window.current().getDocument().createElement("span");
        ripple.setClassName("mdc-list-item__ripple");
        item.appendChild(ripple);

        if (iconName != null && !iconName.isEmpty()) {
            HTMLElement icon = (HTMLElement) Window.current().getDocument().createElement("span");
            icon.setClassName("mdc-list-item__graphic material-icons");
            icon.setAttribute("aria-hidden", "true");
            icon.setInnerText(iconName);
            item.appendChild(icon);
        }

        HTMLElement textSpan = (HTMLElement) Window.current().getDocument().createElement("span");
        textSpan.setClassName("mdc-list-item__text");
        textSpan.setInnerText(text);
        item.appendChild(textSpan);

        if (listener != null) {
            item.addEventListener("click", listener);
        }
        list.appendChild(item);
    }

    /** Convenience overload without an icon. */
    public void addItem(String text, EventListener<?> listener) {
        addItem(text, null, listener);
    }

    public void clear() {
        list.setInnerText("");
    }

    public void setDense(boolean dense) {
        String cls = list.getClassName();
        if (dense && !cls.contains("mdc-list--dense")) {
            list.setClassName(cls + " mdc-list--dense");
        } else if (!dense) {
            list.setClassName(
                cls.replace(" mdc-list--dense", "").replace("mdc-list--dense", "").trim());
        }
    }
}
