package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.Assert.*;

/**
 * Tests for constraint-annotation driven validator generation in {@link BindableProcessor}.
 */
public class ValidationProcessorTest {

    private static JavaFileObject modelWith(String... fieldLines) {
        String[] lines = new String[7 + fieldLines.length];
        lines[0] = "package dev.verrai.processor.test;";
        lines[1] = "import dev.verrai.api.Bindable;";
        lines[2] = "import dev.verrai.api.validation.*;";
        lines[3] = "@Bindable";
        lines[4] = "public class TestModel {";
        int i = 5;
        for (String fl : fieldLines) {
            lines[i++] = "    " + fl;
        }
        lines[i++] = "}";
        lines[i] = ""; // padding
        return JavaFileObjects.forSourceLines("dev.verrai.processor.test.TestModel", lines);
    }

    @Test
    public void notNull_generatesNullCheck() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.NotNull;",
            "@Bindable",
            "public class TestModel {",
            "    @NotNull",
            "    public String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        assertThat(c).generatedSourceFile("dev.verrai.processor.test.TestModel_Validator");
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should contain null check", content.contains("value == null"));
    }

    @Test
    public void notEmpty_generatesEmptyCheck() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.NotEmpty;",
            "@Bindable",
            "public class TestModel {",
            "    @NotEmpty",
            "    public String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should contain isEmpty check", content.contains("isEmpty()"));
    }

    @Test
    public void size_generatesLengthBoundsCheck() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.Size;",
            "@Bindable",
            "public class TestModel {",
            "    @Size(min = 2, max = 50)",
            "    public String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should contain length < 2", content.contains("length() < 2"));
        assertTrue("Should contain length > 50", content.contains("length() > 50"));
    }

    @Test
    public void pattern_generatesRegexCheck() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.Pattern;",
            "@Bindable",
            "public class TestModel {",
            "    @Pattern(regexp = \"[A-Z]+\")",
            "    public String code;",
            "    public String getCode() { return code; }",
            "    public void setCode(String v) { this.code = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should contain Pattern.matches", content.contains("Pattern.matches"));
        assertTrue("Should contain the regexp", content.contains("[A-Z]+"));
    }

    @Test
    public void noConstraints_doesNotGenerateValidator() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "@Bindable",
            "public class TestModel {",
            "    public String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        assertFalse("Validator should NOT be generated",
                c.generatedSourceFile("dev.verrai.processor.test.TestModel_Validator").isPresent());
    }

    @Test
    public void validator_implementsValidatorInterface() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.NotNull;",
            "@Bindable",
            "public class TestModel {",
            "    @NotNull",
            "    public String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should implement Validator", content.contains("implements Validator"));
        assertTrue("Should have static validateField method", content.contains("public static"));
        assertTrue("Should have validate override", content.contains("public ValidationResult validate"));
    }

    @Test
    public void validator_hasValidateFieldSwitchPerField() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.test.TestModel",
            "package dev.verrai.processor.test;",
            "import dev.verrai.api.Bindable;",
            "import dev.verrai.api.validation.NotNull;",
            "import dev.verrai.api.validation.NotEmpty;",
            "@Bindable",
            "public class TestModel {",
            "    @NotNull public String name;",
            "    @NotEmpty public String email;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "    public String getEmail() { return email; }",
            "    public void setEmail(String v) { this.email = v; }",
            "}"
        );

        Compilation c = javac().withProcessors(new BindableProcessor()).compile(source);
        assertThat(c).succeeded();
        String content = ProcessorTestHelper.generatedSource(c, "dev.verrai.processor.test.TestModel_Validator");
        assertTrue("Should have case for 'name'", content.contains("case \"name\""));
        assertTrue("Should have case for 'email'", content.contains("case \"email\""));
    }
}
