package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.Assert.assertFalse;

public class IOCProcessorTest {

    /** Asserts no WARNING diagnostic contains {@code text}. */
    private static void assertNoWarningContaining(Compilation compilation, String text) {
        for (javax.tools.Diagnostic<?> d : compilation.diagnostics()) {
            if (d.getKind() == javax.tools.Diagnostic.Kind.WARNING) {
                String msg = d.getMessage(java.util.Locale.ENGLISH);
                assertFalse("Unexpected warning containing '" + text + "': " + msg,
                        msg.contains(text));
            }
        }
    }

    // ── @Dependent warning tests ──────────────────────────────────────────────

    /**
     * Injecting a type with @Dependent must NOT produce a warning —
     * its module will have generated a factory.
     */
    @Test
    public void testNoWarning_whenInjectedTypeIsDependent() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyWidget",
            "package dev.verrai.processor;",
            "import jakarta.enterprise.context.Dependent;",
            "@Dependent",
            "public class MyWidget {}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.WidgetPage",
            "package dev.verrai.processor;",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "import jakarta.inject.Inject;",
            "@ApplicationScoped",
            "public class WidgetPage {",
            "    @Inject public MyWidget widget;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(widget, page);

        assertThat(compilation).succeeded();
        // No @Dependent warning should appear for a properly scoped widget
        assertNoWarningContaining(compilation, "no factory will be generated");
    }

    /**
     * Injecting a type with no scope annotation should emit a WARNING
     * because no factory will be generated, leading to a runtime failure.
     */
    @Test
    public void testWarning_whenInjectedTypeHasNoScopeAnnotation() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.UnscopedWidget",
            "package dev.verrai.processor;",
            "public class UnscopedWidget {}"  // no @Dependent, no @ApplicationScoped
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.UnscopedPage",
            "package dev.verrai.processor;",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "import jakarta.inject.Inject;",
            "@ApplicationScoped",
            "public class UnscopedPage {",
            "    @Inject public UnscopedWidget widget;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(widget, page);

        // Compilation may fail (generated factory references non-existent UnscopedWidget_Factory)
        // — that is exactly the problem we are warning about. Just assert the warning fires.
        assertThat(compilation).hadWarningContaining("@Dependent");
        assertThat(compilation).hadWarningContaining("UnscopedWidget");
    }

    /**
     * Injecting a type with @ApplicationScoped must NOT produce a warning.
     */
    @Test
    public void testNoWarning_whenInjectedTypeIsApplicationScoped() {
        JavaFileObject service = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ScopedService",
            "package dev.verrai.processor;",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "@ApplicationScoped",
            "public class ScopedService {}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ScopedPage",
            "package dev.verrai.processor;",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "import jakarta.inject.Inject;",
            "@ApplicationScoped",
            "public class ScopedPage {",
            "    @Inject public ScopedService service;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(service, page);

        assertThat(compilation).succeeded();
        assertNoWarningContaining(compilation, "no factory will be generated");
    }

    @Test
    public void testSimpleBean() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.SimpleBean",
            "package dev.verrai.processor;",
            "",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "",
            "@ApplicationScoped",
            "public class SimpleBean {",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(source);

        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("dev.verrai.processor.SimpleBean_Factory");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimpleBean_Factory")
            .contentsAsUtf8String()
            .contains("getInstance()");
    }

    @Test
    public void testInheritanceInjection() {
        JavaFileObject service = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyService",
            "package dev.verrai.processor;",
            "",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "",
            "@ApplicationScoped",
            "public class MyService {}"
        );

        JavaFileObject parent = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ParentBean",
            "package dev.verrai.processor;",
            "",
            "import jakarta.inject.Inject;",
            "",
            "public class ParentBean {",
            "    @Inject",
            "    public MyService service;",
            "}"
        );

        JavaFileObject child = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ChildBean",
            "package dev.verrai.processor;",
            "",
            "import jakarta.enterprise.context.ApplicationScoped;",
            "",
            "@ApplicationScoped",
            "public class ChildBean extends ParentBean {",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(service, parent, child);

        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("dev.verrai.processor.ChildBean_Factory");

        // Should contain injection for parent field
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.ChildBean_Factory")
            .contentsAsUtf8String()
            .contains("bean.service =");
    }
}
