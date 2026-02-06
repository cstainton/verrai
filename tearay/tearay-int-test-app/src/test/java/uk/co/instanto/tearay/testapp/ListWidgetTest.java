package uk.co.instanto.tearay.testapp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ListWidgetTest {

    @Test
    public void testListWidget() {
        StringListComponent list = StringListComponent_Factory.getInstance();
        assertNotNull(list);

        list.setItems(Arrays.asList("Item 1", "Item 2"));

        assertEquals(2, list.element.getChildNodes().getLength());
        assertEquals("Item 1", ((org.teavm.jso.dom.html.HTMLElement)list.element.getChildNodes().get(0)).getInnerText());
        assertEquals("Item 2", ((org.teavm.jso.dom.html.HTMLElement)list.element.getChildNodes().get(1)).getInnerText());
    }
}
