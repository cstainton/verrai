package dev.verrai.material;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.events.EventListener;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design chip widget (MDC Chip pattern).
 * Supports optional leading icon, click handling, and selected state.
 */
@Dependent
public class MaterialChip extends MaterialWidget {

    private final HTMLElement textEl;
    private HTMLElement iconEl;
    private boolean selected = false;

    public MaterialChip() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-chip");
        this.element.setAttribute("role", "row");

        HTMLElement ripple = (HTMLElement) Window.current().getDocument().createElement("span");
        ripple.setClassName("mdc-chip__ripple");
        this.element.appendChild(ripple);

        textEl = (HTMLElement) Window.current().getDocument().createElement("span");
        textEl.setClassName("mdc-chip__text");
        this.element.appendChild(textEl);
    }

    public void setText(String text) {
        textEl.setInnerText(text);
    }

    public String getText() {
        return textEl.getInnerText();
    }

    public void setIcon(String materialIconName) {
        if (iconEl == null) {
            iconEl = (HTMLElement) Window.current().getDocument().createElement("span");
            iconEl.setClassName("mdc-chip__icon mdc-chip__icon--leading material-icons");
            this.element.insertBefore(iconEl, textEl);
        }
        iconEl.setInnerText(materialIconName);
    }

    public void addClickListener(EventListener<?> listener) {
        this.element.addEventListener("click", listener);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        String cls = this.element.getClassName();
        if (selected && !cls.contains("mdc-chip--selected")) {
            this.element.setClassName(cls + " mdc-chip--selected");
        } else if (!selected) {
            this.element.setClassName(
                cls.replace(" mdc-chip--selected", "").replace("mdc-chip--selected", "").trim());
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
