package dev.verrai.sample;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.browser.Window;
import dev.verrai.material.*;
import dev.verrai.api.*;
import jakarta.inject.Inject;

@Page(role = "material-demo")
@Templated
public class MaterialDemoPage {

    @Inject
    public Navigation navigation;

    @RootElement
    public HTMLElement element;

    @Inject @DataField
    public MaterialDrawer drawer;

    @Inject @DataField
    public MaterialTextField nameInput;

    @Inject @DataField
    public MaterialButton toggleButton;

    @Inject @DataField
    public MaterialButton openDialogBtn;

    @Inject @DataField
    public MaterialButton showAlertBtn;

    @Inject @DataField
    public MaterialButton backBtn;

    @Inject @DataField
    public MaterialFAB fab;

    @Inject @DataField
    public MaterialMenu menu;

    @Inject @DataField
    public MaterialCard demoCard;

    @Inject @DataField
    public MaterialProgress progressBar;

    @Inject @DataField
    public MaterialChip chipSet;

    @Inject @DataField
    public MaterialList demoList;

    @Inject @DataField
    public MaterialAlert snackbarAlert;

    @Inject @DataField
    public MaterialDialog confirmDialog;

    @EventHandler("backBtn")
    public void onBack() {
        navigation.goTo("dashboard");
    }

    @PageShowing
    public void onShow() {
        // Drawer
        drawer.addItem("Inbox", e -> Window.alert("Inbox clicked"));
        drawer.addItem("Star", e -> Window.alert("Star clicked"));
        drawer.initialize();

        // Buttons
        toggleButton.setText("Open Drawer");
        toggleButton.addClickListener(e -> drawer.open());

        openDialogBtn.setText("Open Dialog");
        openDialogBtn.addClickListener(e -> {
            Window.current().getDocument().getBody().appendChild(confirmDialog.getElement());
            confirmDialog.open();
        });

        showAlertBtn.setText("Show Snackbar");
        showAlertBtn.addClickListener(e -> {
            Window.current().getDocument().getBody().appendChild(snackbarAlert.getElement());
            snackbarAlert.show();
        });

        // FAB
        fab.addClickListener(e -> Window.alert("FAB Action!"));

        // Menu
        menu.addItem("Refresh", e -> Window.alert("Refreshing..."));
        menu.addItem("Settings", e -> Window.alert("Settings..."));
        menu.setAnchor(fab.getElement());

        // Input
        nameInput.setValue("TeaVM User");

        // Card
        demoCard.setTitle("Material Card");
        demoCard.setSubtitle("A demonstrating card");
        demoCard.setText("This card shows the MDC Card pattern with title, subtitle, and body text.");
        MaterialButton cardBtn = new MaterialButton();
        cardBtn.setText("Action");
        cardBtn.addClickListener(e -> Window.alert("Card action clicked"));
        demoCard.addAction(cardBtn);

        // Progress
        progressBar.setValue(0.65);

        // Chip
        chipSet.setText("Material");
        chipSet.setIcon("label");

        // List
        demoList.addItem("Item One", "inbox", null);
        demoList.addItem("Item Two", "star", e -> Window.alert("Star item clicked"));
        demoList.addItem("Item Three", null, null);

        // Snackbar
        snackbarAlert.setText("Operation completed successfully!");
        snackbarAlert.setDuration(4000);

        // Dialog
        confirmDialog.setTitle("Confirm Action");
        confirmDialog.setContent("Are you sure you want to proceed?");
        confirmDialog.addAction("Cancel", e -> confirmDialog.close());
        confirmDialog.addAction("Confirm", e -> {
            confirmDialog.close();
            Window.alert("Action confirmed!");
        });
    }
}
