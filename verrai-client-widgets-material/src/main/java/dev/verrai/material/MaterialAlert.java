package dev.verrai.material;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design alert/notification widget using the MDC Snackbar pattern.
 *
 * <p>Must be appended to the document body before calling {@link #show()}.
 * Automatically dismisses after {@link #setDuration(int)} milliseconds (default 5000 ms).
 *
 * <pre>{@code
 * MaterialAlert alert = new MaterialAlert();
 * alert.setText("File saved successfully");
 * document.getBody().appendChild(alert.getElement());
 * alert.show();
 * }</pre>
 */
@Dependent
public class MaterialAlert extends MaterialWidget {

    private final HTMLElement labelEl;
    private MDCSnackbar instance;
    private int duration = 5000;

    public MaterialAlert() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-snackbar");

        HTMLElement surface = (HTMLElement) Window.current().getDocument().createElement("div");
        surface.setClassName("mdc-snackbar__surface");
        surface.setAttribute("role", "status");
        surface.setAttribute("aria-relevant", "additions");

        labelEl = (HTMLElement) Window.current().getDocument().createElement("div");
        labelEl.setClassName("mdc-snackbar__label");
        labelEl.setAttribute("aria-atomic", "false");
        surface.appendChild(labelEl);

        HTMLElement actions = (HTMLElement) Window.current().getDocument().createElement("div");
        actions.setClassName("mdc-snackbar__actions");
        surface.appendChild(actions);

        this.element.appendChild(surface);
    }

    public void setText(String text) {
        labelEl.setInnerText(text);
    }

    public String getText() {
        return labelEl.getInnerText();
    }

    /** Sets the auto-dismiss duration in milliseconds (default 5000). */
    public void setDuration(int ms) {
        this.duration = ms;
    }

    /** Shows the snackbar. Lazily initialises the MDC instance. */
    public void show() {
        ensureInstance();
        if (instance != null) {
            instance.setTimeoutMs(duration);
            instance.open();
        }
    }

    /** Hides the snackbar programmatically. */
    public void hide() {
        if (instance != null) {
            instance.close();
        }
    }

    private void ensureInstance() {
        if (instance == null && this.element.getParentNode() != null) {
            instance = MDCSnackbar.attachTo(this.element);
        }
    }

    abstract static class MDCSnackbar implements JSObject {

        @JSBody(params = {"element"}, script =
            "if (typeof mdc !== 'undefined' && mdc.snackbar) { "
            + "return new mdc.snackbar.MDCSnackbar(element); } return null;")
        static native MDCSnackbar attachTo(HTMLElement element);

        @JSBody(script = "this.open();")
        abstract void open();

        @JSBody(script = "this.close();")
        abstract void close();

        @JSProperty("timeoutMs")
        abstract void setTimeoutMs(int ms);
    }
}
