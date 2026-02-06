package uk.co.instanto.tearay.testapp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import uk.co.instanto.tearay.api.impl.BeanLifecycleManager;
import uk.co.instanto.tearay.api.impl.EventBus;
import static org.junit.Assert.*;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class LifecycleTest {

    @Test
    public void testObserverUnsubscription() {
        LifecycleBean bean = LifecycleBean_Factory.getInstance();
        assertNotNull(bean);
        assertEquals(0, bean.eventsObserved);

        // Fire event -> should observe
        EventBus.getInstance().fire("Event 1");
        assertEquals(1, bean.eventsObserved);

        // Destroy bean
        BeanLifecycleManager.getInstance().destroy(bean);

        // Fire event -> should NOT observe
        EventBus.getInstance().fire("Event 2");
        assertEquals(1, bean.eventsObserved);
    }
}
