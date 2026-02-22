package dev.verrai.sample;

import dev.verrai.api.DataField;
import dev.verrai.api.EventHandler;
import dev.verrai.api.Navigation;
import dev.verrai.api.Page;
import dev.verrai.api.PageShowing;
import dev.verrai.api.RootElement;
import dev.verrai.api.Templated;
import dev.verrai.bootstrap.Button;
import dev.verrai.bootstrap.TableWidget;
import jakarta.inject.Inject;
import org.teavm.jso.dom.html.HTMLElement;

@Page(role = "tabledemo")
@Templated
public class TableDemoPage {

    @RootElement
    public HTMLElement element;

    @Inject
    public Navigation navigation;

    @Inject @DataField
    public TableWidget demoTable;

    @Inject @DataField
    public Button backBtn;

    @PageShowing
    public void onShow() {
        demoTable.clearBody();
        demoTable.setHeaders("ID", "Name", "Department", "Role", "Status");
        demoTable.setSortable(true);
        demoTable.setFilterable(true);
        demoTable.setPageSize(5);

        demoTable.addRow("1", "Alice Johnson", "Engineering", "Senior Dev", "Active");
        demoTable.addRow("2", "Bob Smith", "Marketing", "Manager", "Active");
        demoTable.addRow("3", "Charlie Brown", "Engineering", "Junior Dev", "Active");
        demoTable.addRow("4", "Diana Prince", "HR", "Director", "Inactive");
        demoTable.addRow("5", "Edward Norton", "Engineering", "Lead Dev", "Active");
        demoTable.addRow("6", "Fiona Green", "Design", "UX Lead", "Active");
        demoTable.addRow("7", "George Miller", "Engineering", "DevOps", "Active");
        demoTable.addRow("8", "Hannah Lee", "Marketing", "Analyst", "Inactive");

        // Demonstrate widget row: last two cells have action buttons
        Button editBtn = new Button();
        editBtn.setText("Edit");
        editBtn.setType(Button.Type.PRIMARY);
        editBtn.addClickListener(e -> org.teavm.jso.browser.Window.alert("Edit clicked"));

        Button deleteBtn = new Button();
        deleteBtn.setText("Delete");
        deleteBtn.setType(Button.Type.DANGER);
        deleteBtn.addClickListener(e -> org.teavm.jso.browser.Window.alert("Delete clicked"));

        // Widget row: sparse cells — last two carry buttons
        demoTable.addWidgetRow(null, null, null, editBtn, deleteBtn);
    }

    @EventHandler("backBtn")
    public void onBack() {
        navigation.goTo("dashboard");
    }
}
