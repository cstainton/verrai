package com.example;

import com.example.errai.api.DataField;
import com.example.errai.api.Templated;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.browser.Window;

@Templated
public class MyComponent {

    @DataField
    public HTMLButtonElement submit;

    @DataField
    public HTMLElement output;

    public void init() {
        // In a real app, DI would handle this.
        // Here we invoke the generated Binder manually.
        HTMLElement root = MyComponent_Binder.bind(this);

        Window.current().getDocument().getBody().appendChild(root);

        submit.addEventListener("click", evt -> {
            output.setInnerHTML("Hello from TeaVM! Time: " + System.currentTimeMillis());
        });
    }
}
