package dev.verrai.material;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.events.EventListener;
import dev.verrai.api.IsWidget;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design dialog widget (MDC Dialog pattern).
 *
 * <p>Must be appended to the document body before calling {@link #open()}.
 *
 * <pre>{@code
 * MaterialDialog dialog = new MaterialDialog();
 * dialog.setTitle("Confirm");
 * dialog.setContent("Are you sure you want to delete this item?");
 * dialog.addAction("Cancel", e -> dialog.close());
 * dialog.addAction("Delete", e -> { performDelete(); dialog.close(); });
 * document.getBody().appendChild(dialog.getElement());
 * dialog.open();
 * }</pre>
 */
@Dependent
public class MaterialDialog extends MaterialWidget {

    private final HTMLElement titleEl;
    private final HTMLElement contentEl;
    private final HTMLElement actionsEl;
    private MDCDialog instance;

    public MaterialDialog() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-dialog");

        HTMLElement container = (HTMLElement) Window.current().getDocument().createElement("div");
        container.setClassName("mdc-dialog__container");

        HTMLElement surface = (HTMLElement) Window.current().getDocument().createElement("div");
        surface.setClassName("mdc-dialog__surface");
        surface.setAttribute("role", "alertdialog");
        surface.setAttribute("aria-modal", "true");

        titleEl = (HTMLElement) Window.current().getDocument().createElement("h2");
        titleEl.setClassName("mdc-dialog__title");
        surface.appendChild(titleEl);

        contentEl = (HTMLElement) Window.current().getDocument().createElement("div");
        contentEl.setClassName("mdc-dialog__content");
        surface.appendChild(contentEl);

        actionsEl = (HTMLElement) Window.current().getDocument().createElement("div");
        actionsEl.setClassName("mdc-dialog__actions");
        surface.appendChild(actionsEl);

        container.appendChild(surface);
        this.element.appendChild(container);

        // Scrim (backdrop)
        HTMLElement scrim = (HTMLElement) Window.current().getDocument().createElement("div");
        scrim.setClassName("mdc-dialog__scrim");
        this.element.appendChild(scrim);
    }

    public void setTitle(String title) {
        titleEl.setInnerText(title);
    }

    public String getTitle() {
        return titleEl.getInnerText();
    }

    public void setContent(String text) {
        contentEl.setInnerText(text);
    }

    public void addContent(IsWidget widget) {
        if (widget != null && widget.getElement() != null) {
            contentEl.appendChild(widget.getElement());
        }
    }

    /**
     * Adds a text action button to the dialog's action bar.
     *
     * @param label    button label
     * @param listener click listener (may include {@code dialog.close()})
     */
    public void addAction(String label, EventListener<?> listener) {
        HTMLElement btn = (HTMLElement) Window.current().getDocument().createElement("button");
        btn.setClassName("mdc-button mdc-dialog__button");
        btn.setAttribute("type", "button");

        HTMLElement ripple = (HTMLElement) Window.current().getDocument().createElement("span");
        ripple.setClassName("mdc-button__ripple");
        btn.appendChild(ripple);

        HTMLElement labelEl = (HTMLElement) Window.current().getDocument().createElement("span");
        labelEl.setClassName("mdc-button__label");
        labelEl.setInnerText(label);
        btn.appendChild(labelEl);

        if (listener != null) {
            btn.addEventListener("click", listener);
        }
        actionsEl.appendChild(btn);
    }

    /** Opens the dialog. Lazily initialises the MDC instance. */
    public void open() {
        ensureInstance();
        if (instance != null) {
            instance.open();
        }
    }

    /** Closes the dialog. */
    public void close() {
        if (instance != null) {
            instance.close();
        }
    }

    private void ensureInstance() {
        if (instance == null && this.element.getParentNode() != null) {
            instance = MDCDialog.attachTo(this.element);
        }
    }

    abstract static class MDCDialog implements JSObject {

        @JSBody(params = {"element"}, script =
            "if (typeof mdc !== 'undefined' && mdc.dialog) { " +
            "return new mdc.dialog.MDCDialog(element); } return null;")
        static native MDCDialog attachTo(HTMLElement element);

        @JSBody(script = "this.open();")
        abstract void open();

        @JSBody(script = "this.close();")
        abstract void close();
    }
}
