package uk.co.instanto.tearay.widgets;

import org.teavm.jso.browser.Window;
import uk.co.instanto.tearay.api.TakesValue;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public abstract class ListComponent<M, W extends Widget & TakesValue<M>> extends Widget implements TakesValue<List<M>> {

    @Inject
    public Provider<W> itemProvider;

    private List<M> items;

    public ListComponent() {
        this.element = Window.current().getDocument().createElement("div");
    }

    @Override
    public void setValue(List<M> items) {
        setItems(items);
    }

    @Override
    public List<M> getValue() {
        return items;
    }

    public void setItems(List<M> items) {
        this.items = items;
        this.element.setInnerHTML("");
        if (items != null) {
            for (M item : items) {
                W widget = itemProvider.get();
                widget.setValue(item);
                this.element.appendChild(widget.element);
            }
        }
    }
}
