package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;

public class SecurityProcessorTest {

    @Test
    public void testRestrictedAccessWithoutProvider() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.RestrictedPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Page;",
            "import dev.verrai.api.RestrictedAccess;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Page(role = \"admin\")",
            "@RestrictedAccess(roles = {\"ADMIN\"})",
            "public class RestrictedPage {",
            "    public HTMLElement element;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new NavigationProcessor(), new IOCProcessor())
            .compile(source);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.impl.NavigationImpl")
            .contentsAsUtf8String()
            .contains("Window.alert(\"Access Denied: Security provider not configured\")");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.impl.NavigationImpl")
            .contentsAsUtf8String()
            .contains("return;");
    }

    @Test
    public void testRestrictedAccessWithProvider() {
        JavaFileObject pageSource = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.RestrictedPageWithProvider",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Page;",
            "import dev.verrai.api.RestrictedAccess;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Page(role = \"user\")",
            "@RestrictedAccess(roles = {\"USER\"})",
            "public class RestrictedPageWithProvider {",
            "    public HTMLElement element;",
            "}"
        );

        JavaFileObject providerSource = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MySecurityProvider",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.SecurityProvider;",
            "import dev.verrai.api.ApplicationScoped;",
            "",
            "@ApplicationScoped",
            "public class MySecurityProvider implements SecurityProvider {",
            "    public boolean hasRole(String role) { return true; }",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new NavigationProcessor(), new IOCProcessor())
            .compile(pageSource, providerSource);

        assertThat(compilation).succeeded();

        // Check for runtime null check
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.impl.NavigationImpl")
            .contentsAsUtf8String()
            .contains("if (this.securityProvider == null)");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.impl.NavigationImpl")
            .contentsAsUtf8String()
            .contains("Window.alert(\"Access Denied: Security provider missing\")");

        // Check for role check
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.impl.NavigationImpl")
            .contentsAsUtf8String()
            .contains("if (!this.securityProvider.hasRole(\"USER\"))");
    }
}
