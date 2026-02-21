package dev.verrai.processor.model;

import dev.verrai.security.RestrictedAccess;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

public class PageDefinition {
    private final TypeElement typeElement;
    private final String role;
    private final RestrictedAccess restrictedAccess;
    private final List<VariableElement> pageStateFields;
    private final List<ExecutableElement> pageShowingMethods;
    private final List<ExecutableElement> pageHidingMethods;
    private final List<ExecutableElement> pageHiddenMethods;
    private final boolean startingPage;
    private final String varName;

    public PageDefinition(TypeElement typeElement, String role, RestrictedAccess restrictedAccess,
            List<VariableElement> pageStateFields, List<ExecutableElement> pageShowingMethods,
            List<ExecutableElement> pageHidingMethods, List<ExecutableElement> pageHiddenMethods,
            boolean startingPage) {
        this.typeElement = typeElement;
        this.role = role;
        this.restrictedAccess = restrictedAccess;
        this.pageStateFields = pageStateFields;
        this.pageShowingMethods = pageShowingMethods;
        this.pageHidingMethods = pageHidingMethods;
        this.pageHiddenMethods = pageHiddenMethods;
        this.startingPage = startingPage;
        this.varName = role.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String getRole() {
        return role;
    }

    public RestrictedAccess getRestrictedAccess() {
        return restrictedAccess;
    }

    public List<VariableElement> getPageStateFields() {
        return pageStateFields;
    }

    public List<ExecutableElement> getPageShowingMethods() {
        return pageShowingMethods;
    }

    public List<ExecutableElement> getPageHidingMethods() {
        return pageHidingMethods;
    }

    public List<ExecutableElement> getPageHiddenMethods() {
        return pageHiddenMethods;
    }

    public boolean isStartingPage() {
        return startingPage;
    }

    public String getVarName() {
        return varName;
    }
}
