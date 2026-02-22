package dev.verrai.sample;

import dev.verrai.ui.ListWidget;
import dev.verrai.ui.HasModel;
import dev.verrai.api.Templated;
import jakarta.enterprise.context.Dependent;

// We need to extend ListWidget for CDI to inject the provider
@Dependent
public class TaskListWidget extends ListWidget<Task, TaskWidget> {
    public TaskListWidget() {
        super("div");
        getElement().setClassName("list-group");
    }
}
