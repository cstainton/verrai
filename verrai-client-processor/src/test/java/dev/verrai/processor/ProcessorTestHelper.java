package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;
import java.io.IOException;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Utility class for Verrai annotation-processor tests.
 * Reduces boilerplate when constructing synthetic source files and running compilations.
 */
public final class ProcessorTestHelper {

    private ProcessorTestHelper() {}

    // ── Source builders ───────────────────────────────────────────────────────

    /** Minimal {@code @Page} class with a bare {@code HTMLElement element} field. */
    public static JavaFileObject page(String role) {
        String name = sanitize(role) + "Page";
        return JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test." + name,
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Page;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "@Page(role = \"" + role + "\")",
            "public class " + name + " {",
            "    public HTMLElement element;",
            "}"
        );
    }

    /** Like {@link #page(String)} but with {@code startingPage = true}. */
    public static JavaFileObject startingPage(String role) {
        String name = sanitize(role) + "Page";
        return JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test." + name,
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Page;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "@Page(role = \"" + role + "\", startingPage = true)",
            "public class " + name + " {",
            "    public HTMLElement element;",
            "}"
        );
    }

    // ── Compilation helpers ───────────────────────────────────────────────────

    /** Compiles sources through all four Verrai processors. */
    public static Compilation compile(JavaFileObject... sources) {
        return javac()
            .withProcessors(
                new NavigationProcessor(),
                new IOCProcessor(),
                new TemplatedProcessor(),
                new BindableProcessor())
            .compile(sources);
    }

    /** Compiles sources through NavigationProcessor + IOCProcessor only. */
    public static Compilation compileWithNavigation(JavaFileObject... sources) {
        return javac()
            .withProcessors(new NavigationProcessor(), new IOCProcessor())
            .compile(sources);
    }

    /** Compiles sources through TemplatedProcessor + BindableProcessor only. */
    public static Compilation compileWithTemplated(JavaFileObject... sources) {
        return javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(sources);
    }

    // ── Generated-source helpers ──────────────────────────────────────────────

    /**
     * Returns the source content of a generated file by fully-qualified name.
     *
     * @throws RuntimeException if the file was not generated
     */
    public static String generatedSource(Compilation compilation, String fqn) {
        try {
            return compilation.generatedSourceFile(fqn)
                .get()
                .getCharContent(false)
                .toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not read generated file: " + fqn, e);
        }
    }

    /**
     * Asserts that the generated source file {@code fqn} contains {@code snippet}.
     * Delegates to Google Truth for a clear failure message.
     */
    public static void assertContains(Compilation compilation, String fqn, String snippet) {
        assertThat(compilation)
            .generatedSourceFile(fqn)
            .contentsAsUtf8String()
            .contains(snippet);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Converts a role string to a valid Java identifier segment.
     * E.g. {@code "my-role"} → {@code "MyRole"}.
     */
    static String sanitize(String role) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char ch : role.toCharArray()) {
            if (!Character.isLetterOrDigit(ch)) {
                capitalize = true;
            } else {
                sb.append(capitalize ? Character.toUpperCase(ch) : ch);
                capitalize = false;
            }
        }
        return sb.toString();
    }
}
