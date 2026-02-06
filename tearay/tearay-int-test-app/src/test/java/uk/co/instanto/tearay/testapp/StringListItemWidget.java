package uk.co.instanto.tearay.testapp;

import uk.co.instanto.tearay.api.TakesValue;
import uk.co.instanto.tearay.api.cdi.Dependent;
import uk.co.instanto.tearay.widgets.Widget;
import org.teavm.jso.browser.Window;

@Dependent
public class StringListItemWidget extends Widget implements TakesValue<String> {

    public StringListItemWidget() {
        this.element = Window.current().getDocument().createElement("li");
    }

    @Override
    public void setValue(String value) {
        this.element.setInnerText(value);
    }

    @Override
    public String getValue() {
        return this.element.getInnerText();
    }
}
