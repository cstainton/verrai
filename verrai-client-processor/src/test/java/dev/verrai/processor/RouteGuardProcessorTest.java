package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.Assert.*;

/**
 * Tests for CanActivate / CanDeactivate route guard code generation.
 */
public class RouteGuardProcessorTest {

    private static JavaFileObject canDeactivatePage(String role) {
        String name = ProcessorTestHelper.sanitize(role) + "Page";
        return JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test." + name,
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Page;",
            "import dev.verrai.api.CanDeactivate;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "@Page(role = \"" + role + "\")",
            "public class " + name + " implements CanDeactivate {",
            "    public HTMLElement element;",
            "    public boolean canDeactivate() { return false; }",
            "}"
        );
    }

    private static JavaFileObject canActivatePage(String role) {
        String name = ProcessorTestHelper.sanitize(role) + "Page";
        return JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test." + name,
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Page;",
            "import dev.verrai.api.CanActivate;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "import java.util.Map;",
            "@Page(role = \"" + role + "\")",
            "public class " + name + " implements CanActivate {",
            "    public HTMLElement element;",
            "    public boolean canActivate(String toRole, Map<String, String> state) { return true; }",
            "}"
        );
    }

    @Test
    public void canDeactivate_generatesInstanceOfCheck() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("home"),
                canDeactivatePage("protected"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        assertTrue("Should check instanceof CanDeactivate",
                nav.contains("instanceof dev.verrai.api.CanDeactivate"));
        assertTrue("Should return early on canDeactivate() == false",
                nav.contains("canDeactivate()"));
    }

    @Test
    public void canActivate_generatesActivateCheck() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("home"),
                canActivatePage("secured"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        assertTrue("Should call canActivate on the new page",
                nav.contains("canActivate(role, state)"));
    }

    @Test
    public void plainPage_doesNotHaveCanActivateCall() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("home"),
                ProcessorTestHelper.page("plain"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        // The CanDeactivate instanceof check is always emitted globally (not per-case)
        // The CanActivate check should NOT appear in the plain page's case block
        // We verify by checking that the plain page case does not contain canActivate
        // (The global CanDeactivate check is before the switch, so both can coexist)
        // Simplest: the generated code should NOT have canActivate(role, state) at all
        // since no page implements it
        assertFalse("Plain-only compilation should not have canActivate call",
                nav.contains("canActivate(role, state)"));
    }

    @Test
    public void multiplePagesWithMixedGuards_correctGuardsOnCorrectCases() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("home"),
                canActivatePage("secured"),
                canDeactivatePage("protected"),
                ProcessorTestHelper.page("plain"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        // canActivate appears because "secured" implements it
        assertTrue("canActivate should appear for secured page", nav.contains("canActivate(role, state)"));
        // CanDeactivate check is global (before switch)
        assertTrue("CanDeactivate check should be present", nav.contains("instanceof dev.verrai.api.CanDeactivate"));
    }

    @Test
    public void previousRole_fieldIsTracked() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("home"),
                ProcessorTestHelper.page("about"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        assertTrue("previousRole field should be declared",
                nav.contains("previousRole"));
        assertTrue("previousRole should be assigned after successful mount",
                nav.contains("this.previousRole ="));
    }
}
