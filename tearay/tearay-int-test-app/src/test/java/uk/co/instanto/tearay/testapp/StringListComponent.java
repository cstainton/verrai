package uk.co.instanto.tearay.testapp;

import uk.co.instanto.tearay.api.cdi.Dependent;
import uk.co.instanto.tearay.widgets.ListComponent;

@Dependent
public class StringListComponent extends ListComponent<String, StringListItemWidget> {
}
