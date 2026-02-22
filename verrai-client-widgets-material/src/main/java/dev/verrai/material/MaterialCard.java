package dev.verrai.material;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import dev.verrai.api.IsWidget;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design card widget (MDC Card pattern).
 * Provides a title, optional subtitle, body text, arbitrary content, and action buttons.
 */
@Dependent
public class MaterialCard extends MaterialWidget {

    private final HTMLElement primaryAction;
    private final HTMLElement primary;
    private final HTMLElement titleEl;
    private final HTMLElement subtitleEl;
    private final HTMLElement contentEl;
    private final HTMLElement actions;

    public MaterialCard() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-card");

        primaryAction = (HTMLElement) Window.current().getDocument().createElement("div");
        primaryAction.setClassName("mdc-card__primary-action");
        this.element.appendChild(primaryAction);

        primary = (HTMLElement) Window.current().getDocument().createElement("div");
        primary.setClassName("mdc-card__primary");
        primaryAction.appendChild(primary);

        titleEl = (HTMLElement) Window.current().getDocument().createElement("h2");
        titleEl.setClassName("mdc-card__title mdc-typography--headline6");
        primary.appendChild(titleEl);

        subtitleEl = (HTMLElement) Window.current().getDocument().createElement("p");
        subtitleEl.setClassName("mdc-card__subtitle mdc-typography--subtitle2");
        primary.appendChild(subtitleEl);

        contentEl = (HTMLElement) Window.current().getDocument().createElement("div");
        contentEl.setClassName("mdc-card__supporting-text");
        primaryAction.appendChild(contentEl);

        actions = (HTMLElement) Window.current().getDocument().createElement("div");
        actions.setClassName("mdc-card__actions");
        this.element.appendChild(actions);
    }

    public void setTitle(String title) {
        titleEl.setInnerText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleEl.setInnerText(subtitle);
    }

    public void setText(String text) {
        contentEl.setInnerText(text);
    }

    public void addContent(IsWidget widget) {
        if (widget != null && widget.getElement() != null) {
            contentEl.appendChild(widget.getElement());
        }
    }

    public void addAction(MaterialButton btn) {
        if (btn != null && btn.getElement() != null) {
            HTMLElement wrapper = (HTMLElement) Window.current().getDocument().createElement("div");
            wrapper.setClassName("mdc-card__action-buttons");
            wrapper.appendChild(btn.getElement());
            actions.appendChild(wrapper);
        }
    }
}
