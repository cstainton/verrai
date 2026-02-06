package uk.co.instanto.tearay.testapp;

import uk.co.instanto.tearay.api.cdi.Dependent;
import uk.co.instanto.tearay.api.cdi.Observes;

@Dependent
public class LifecycleBean {
    public int eventsObserved = 0;

    public void onEvent(@Observes String event) {
        eventsObserved++;
    }
}
