package dev.verrai.material;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import dev.verrai.api.TakesValue;
import jakarta.enterprise.context.Dependent;

/**
 * Material Design linear progress indicator (MDC LinearProgress pattern).
 *
 * <p>Value range is 0.0 – 1.0. Call {@link #setIndeterminate(boolean)} to show a
 * continuous animation when the exact progress is unknown.
 *
 * <pre>{@code
 * MaterialProgress progress = new MaterialProgress();
 * progress.setValue(0.65);
 * container.appendChild(progress.getElement());
 * }</pre>
 */
@Dependent
public class MaterialProgress extends MaterialWidget implements TakesValue<Double> {

    private MDCLinearProgress instance;
    private double currentValue = 0.0;
    private boolean indeterminate = false;

    public MaterialProgress() {
        this.element = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element.setClassName("mdc-linear-progress");
        this.element.setAttribute("role", "progressbar");
        this.element.setAttribute("aria-valuemin", "0");
        this.element.setAttribute("aria-valuemax", "1");
        this.element.setAttribute("aria-valuenow", "0");

        HTMLElement buffer = (HTMLElement) Window.current().getDocument().createElement("div");
        buffer.setClassName("mdc-linear-progress__buffer");
        HTMLElement bufferBar = (HTMLElement) Window.current().getDocument().createElement("div");
        bufferBar.setClassName("mdc-linear-progress__buffer-bar");
        HTMLElement bufferDots = (HTMLElement) Window.current().getDocument().createElement("div");
        bufferDots.setClassName("mdc-linear-progress__buffer-dots");
        buffer.appendChild(bufferBar);
        buffer.appendChild(bufferDots);

        HTMLElement primary = (HTMLElement) Window.current().getDocument().createElement("div");
        primary.setClassName("mdc-linear-progress__bar mdc-linear-progress__primary-bar");
        HTMLElement primaryInner = (HTMLElement) Window.current().getDocument().createElement("span");
        primaryInner.setClassName("mdc-linear-progress__bar-inner");
        primary.appendChild(primaryInner);

        HTMLElement secondary = (HTMLElement) Window.current().getDocument().createElement("div");
        secondary.setClassName("mdc-linear-progress__bar mdc-linear-progress__secondary-bar");
        HTMLElement secondaryInner = (HTMLElement) Window.current().getDocument().createElement("span");
        secondaryInner.setClassName("mdc-linear-progress__bar-inner");
        secondary.appendChild(secondaryInner);

        this.element.appendChild(buffer);
        this.element.appendChild(primary);
        this.element.appendChild(secondary);
    }

    /** Sets the progress value in range 0.0 – 1.0. */
    @Override
    public void setValue(Double value) {
        if (value == null) value = 0.0;
        this.currentValue = Math.max(0.0, Math.min(1.0, value));
        this.element.setAttribute("aria-valuenow", String.valueOf(currentValue));
        ensureInstance();
        if (instance != null) {
            instance.setProgress(currentValue);
        }
    }

    public void setValue(double value) {
        setValue(Double.valueOf(value));
    }

    @Override
    public Double getValue() {
        return currentValue;
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        ensureInstance();
        if (instance != null) {
            instance.setDeterminate(!indeterminate);
        }
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    private void ensureInstance() {
        if (instance == null && this.element.getParentNode() != null) {
            instance = MDCLinearProgress.attachTo(this.element);
        }
    }

    abstract static class MDCLinearProgress implements JSObject {

        @JSBody(params = {"element"}, script =
            "if (typeof mdc !== 'undefined' && mdc.linearProgress) { " +
            "return new mdc.linearProgress.MDCLinearProgress(element); } return null;")
        static native MDCLinearProgress attachTo(HTMLElement element);

        @JSProperty("progress")
        abstract void setProgress(double progress);

        @JSProperty("determinate")
        abstract void setDeterminate(boolean determinate);
    }
}
