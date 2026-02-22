package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.Assert.*;

/**
 * Smoke tests for {@link ProcessorTestHelper}.
 */
public class ProcessorTestHelperTest {

    @Test
    public void page_producesValidPageSource() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.page("home"));
        assertThat(c).succeeded();
        assertThat(c).generatedSourceFile("dev.verrai.impl.NavigationImpl");
    }

    @Test
    public void startingPage_setsStartingPageFlag() {
        Compilation c = ProcessorTestHelper.compileWithNavigation(
                ProcessorTestHelper.startingPage("login"));
        assertThat(c).succeeded();
        String nav = ProcessorTestHelper.generatedSource(c, "dev.verrai.impl.NavigationImpl");
        assertTrue("Expected goTo(\"login\") in start()", nav.contains("goTo(\"login\")"));
    }

    @Test
    public void sanitize_convertsHyphenatedRole() {
        assertEquals("MyRole", ProcessorTestHelper.sanitize("my-role"));
        assertEquals("HomePageWidget", ProcessorTestHelper.sanitize("home-page-widget"));
        assertEquals("Dashboard", ProcessorTestHelper.sanitize("dashboard"));
    }
}
