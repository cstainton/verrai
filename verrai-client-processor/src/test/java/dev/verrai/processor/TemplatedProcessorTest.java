package dev.verrai.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemplatedProcessorTest {

    @Test
    public void testSimplePageBinding() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.SimplePage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class SimplePage {",
            "    @DataField",
            "    public HTMLElement name;",
            "",
            "    @DataField",
            "    public HTMLElement submit;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(source);

        assertThat(compilation).succeeded();

        // Basic check: file generated
        assertThat(compilation).generatedSourceFile("dev.verrai.processor.SimplePage_Binder");

        // Verify optimized querySelectorAll usage
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimplePage_Binder")
            .contentsAsUtf8String()
            .contains("root.querySelectorAll(\"[data-field]\")");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimplePage_Binder")
            .contentsAsUtf8String()
            .contains("switch (key)");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimplePage_Binder")
            .contentsAsUtf8String()
            .contains("case \"name\":");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimplePage_Binder")
            .contentsAsUtf8String()
            .contains("el_name = candidate;");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.SimplePage_Binder")
            .contentsAsUtf8String()
            .contains("case \"submit\":");
    }

    @Test
    public void testBinderLifecycleIntegration() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyWidget",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "import org.teavm.jso.dom.events.Event;",
            "",
            "public class MyWidget implements IsWidget, TakesValue<String> {",
            "    public HTMLElement getElement() { return null; }",
            "    public void setValue(String value) {}",
            "    public String getValue() { return null; }",
            "    public void onBrowserEvent(Event e) {}",
            "}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyModel",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Bindable;",
            "",
            "@Bindable",
            "public class MyModel {",
            "    private String name;",
            "    public void setName(String name) { this.name = name; }",
            "    public String getName() { return name; }",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.LifecyclePage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import dev.verrai.api.binding.BinderLifecycle;",
            "import dev.verrai.api.binding.Subscription;",
            "import jakarta.inject.Inject;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class LifecyclePage implements BinderLifecycle {",
            "    @Inject @Model",
            "    public MyModel model;",
            "",
            "    @Inject @DataField @Bound",
            "    public MyWidget name;",
            "",
            "    public void addBinding(Subscription s) {}",
            "    public void clearBindings() {}",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(widget, model, page);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.LifecyclePage_Binder")
            .contentsAsUtf8String()
            .contains("((dev.verrai.api.binding.BinderLifecycle) target).addBinding(subscription)");
    }

    @Test
    public void testBindableModelBinding() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyWidget",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "import org.teavm.jso.dom.events.Event;",
            "",
            "public class MyWidget implements IsWidget, TakesValue<String> {",
            "    public HTMLElement getElement() { return null; }",
            "    public void setValue(String value) {}",
            "    public String getValue() { return null; }",
            "    public void onBrowserEvent(Event e) {}",
            "}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyModel",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Bindable;",
            "",
            "@Bindable",
            "public class MyModel {",
            "    private String name;",
            "    public void setName(String name) { this.name = name; }",
            "    public String getName() { return name; }",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.BindablePage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import jakarta.inject.Inject;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class BindablePage {",
            "    @Inject @Model",
            "    public MyModel model;",
            "",
            "    @Inject @DataField @Bound",
            "    public MyWidget name;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(widget, model, page);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.BindablePage_Binder")
            .contentsAsUtf8String()
            .contains("if (target.model instanceof dev.verrai.api.binding.BindableProxy)");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.BindablePage_Binder")
            .contentsAsUtf8String()
            .contains("((dev.verrai.api.binding.BindableProxy) target.model).addPropertyChangeHandler");
    }

    @Test
    public void testElementFieldBinding() {
         JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ElementPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class ElementPage {",
            "    public HTMLElement element;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(source);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.ElementPage_Binder")
            .contentsAsUtf8String()
            .contains("target.element = root");
    }

    @Test
    public void testInheritedElementFieldBinding() {
         JavaFileObject parent = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ParentPage",
            "package dev.verrai.processor;",
            "",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "public class ParentPage {",
            "    public HTMLElement element;",
            "}"
        );

         JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.InheritedPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class InheritedPage extends ParentPage {",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(parent, source);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.InheritedPage_Binder")
            .contentsAsUtf8String()
            .contains("target.element = root");
    }

    @Test
    public void testInputEventForHtmlInputElement() {
        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.InputModel",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Bindable;",
            "",
            "@Bindable",
            "public class InputModel {",
            "    private String name;",
            "    public void setName(String name) { this.name = name; }",
            "    public String getName() { return name; }",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.InputEventPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import jakarta.inject.Inject;",
            "import org.teavm.jso.dom.html.HTMLInputElement;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class InputEventPage {",
            "    @Inject @Model",
            "    public InputModel model;",
            "",
            "    @DataField @Bound",
            "    public HTMLInputElement name;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(model, page);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.InputEventPage_Binder")
            .contentsAsUtf8String()
            .contains("addEventListener(\"input\"");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.InputEventPage_Binder")
            .contentsAsUtf8String()
            .doesNotContain("addEventListener(\"change\"");
    }

    @Test
    public void testNestedPropertyBinding() {
        JavaFileObject address = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.Address",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Bindable;",
            "",
            "@Bindable",
            "public class Address {",
            "    private String street;",
            "    public void setStreet(String street) { this.street = street; }",
            "    public String getStreet() { return street; }",
            "}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.NestedModel",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Bindable;",
            "",
            "@Bindable",
            "public class NestedModel {",
            "    private String name;",
            "    private Address address;",
            "    public void setName(String name) { this.name = name; }",
            "    public String getName() { return name; }",
            "    public void setAddress(Address address) { this.address = address; }",
            "    public Address getAddress() { return address; }",
            "}"
        );

        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.MyWidget",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "import org.teavm.jso.dom.events.Event;",
            "",
            "public class MyWidget implements IsWidget, TakesValue<String> {",
            "    public HTMLElement getElement() { return null; }",
            "    public void setValue(String value) {}",
            "    public String getValue() { return null; }",
            "    public void onBrowserEvent(Event e) {}",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.NestedPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import dev.verrai.api.binding.BinderLifecycle;",
            "import dev.verrai.api.binding.Subscription;",
            "import jakarta.inject.Inject;",
            "",
            "@Templated(\"NestedPage.html\")",
            "public class NestedPage implements BinderLifecycle {",
            "    @Inject @Model",
            "    public NestedModel model;",
            "",
            "    @DataField @Bound",
            "    public MyWidget name;",
            "",
            "    @DataField @Bound(property = \"address.street\")",
            "    public MyWidget street;",
            "",
            "    public void addBinding(Subscription s) {}",
            "    public void clearBindings() {}",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(address, model, widget, page);

        assertThat(compilation).succeeded();

        // Root model listener still attached for "address" property
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.NestedPage_Binder")
            .contentsAsUtf8String()
            .contains("p.equals(\"address\")");

        // Nested proxy listener attached to model.getAddress() for "street"
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.NestedPage_Binder")
            .contentsAsUtf8String()
            .contains("target.model.getAddress() instanceof dev.verrai.api.binding.BindableProxy");

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.NestedPage_Binder")
            .contentsAsUtf8String()
            .contains("p.equals(\"street\")");

        // Both nested subscriptions registered with lifecycle
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.NestedPage_Binder")
            .contentsAsUtf8String()
            .contains("nestedSub_street");
    }

    /**
     * Verifies that when a pre-compiled {@code *_Validator} is available on the classpath
     * (simulating a multi-module build where the model was compiled in a prior module),
     * the TemplatedProcessor weaves inline validation into the change handler.
     */
    @Test
    public void testValidationWovenIn_whenValidatorAvailable() {
        // Stub Bootstrap Widget so ValWidget can extend it — required for the
        // instanceof cast in the generated validation wiring to be type-safe.
        JavaFileObject bootstrapWidget = JavaFileObjects.forSourceLines(
            "dev.verrai.bootstrap.Widget",
            "package dev.verrai.bootstrap;",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "public class Widget implements IsWidget, TakesValue<String> {",
            "    public HTMLElement element;",
            "    public HTMLElement getElement() { return element; }",
            "    public void setValue(String v) {}",
            "    public String getValue() { return null; }",
            "    public void setErrorMessage(String msg) {}",
            "    public void clearErrorMessage() {}",
            "}"
        );

        // ValWidget extends Widget so the generated `instanceof Widget` cast succeeds.
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ValWidget",
            "package dev.verrai.processor;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "public class ValWidget extends dev.verrai.bootstrap.Widget {",
            "    @Override public String getValue() { return null; }",
            "}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ValidatedModel",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Bindable;",
            "@Bindable",
            "public class ValidatedModel {",
            "    private String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        // Provide a pre-written validator, simulating what BindableProcessor would generate
        // in a prior compilation (e.g., the model module compiled before the UI module).
        JavaFileObject validator = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ValidatedModel_Validator",
            "package dev.verrai.processor;",
            "import dev.verrai.api.validation.ValidationResult;",
            "import dev.verrai.api.validation.Validator;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public class ValidatedModel_Validator implements Validator<ValidatedModel> {",
            "    public static ValidationResult validateField(String fieldName, Object value) {",
            "        List<String> errors = new ArrayList<>();",
            "        switch (fieldName) {",
            "            case \"name\":",
            "                if (value == null) errors.add(\"must not be null\");",
            "                break;",
            "            default: break;",
            "        }",
            "        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);",
            "    }",
            "    @Override",
            "    public ValidationResult validate(ValidatedModel model) {",
            "        return validateField(\"name\", model.getName());",
            "    }",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.ValidatedPage",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import jakarta.inject.Inject;",
            "@Templated(\"SimplePage.html\")",
            "public class ValidatedPage {",
            "    @Inject @Model",
            "    public ValidatedModel model;",
            "    @Inject @DataField @Bound",
            "    public ValWidget name;",
            "}"
        );

        // MaterialWidget stub needed so the generated binder's material branch compiles
        JavaFileObject materialWidget = JavaFileObjects.forSourceLines(
            "dev.verrai.material.MaterialWidget",
            "package dev.verrai.material;",
            "import dev.verrai.api.IsWidget;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "public abstract class MaterialWidget implements IsWidget {",
            "    protected HTMLElement element;",
            "    public HTMLElement getElement() { return element; }",
            "    public void setErrorMessage(String msg) {}",
            "    public void clearErrorMessage() {}",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(bootstrapWidget, widget, model, validator, page, materialWidget);

        assertThat(compilation).succeeded();

        String binderSource = ProcessorTestHelper.generatedSource(compilation,
            "dev.verrai.processor.ValidatedPage_Binder");

        assertTrue("Binder should call validateField", binderSource.contains("validateField"));
        assertTrue("Binder should check _vr.isValid()", binderSource.contains("_vr.isValid()"));
        // ValidationAware branch emitted first
        assertTrue("Binder should check ValidationAware", binderSource.contains("ValidationAware"));
        assertTrue("Binder should call onValidationResult", binderSource.contains("onValidationResult"));
        // Duck-typed fallback: direct call on the concrete field type (ValWidget extends Widget)
        assertTrue("Binder should call setErrorMessage in fallback", binderSource.contains("setErrorMessage"));
        assertFalse("Binder should not use _widgetRef_ Object cast", binderSource.contains("_widgetRef_"));
        assertFalse("Binder should not reference bootstrap package", binderSource.contains("dev.verrai.bootstrap"));
        assertFalse("Binder should not reference material package", binderSource.contains("dev.verrai.material"));
        // Model update gated on valid result
        assertTrue("Binder should gate model update on isValid", binderSource.contains("_vr.isValid()"));
    }

    /**
     * Verifies that when no {@code *_Validator} exists (model has no constraints),
     * the TemplatedProcessor generates a plain setter call with no validation wrapping.
     */
    @Test
    public void testNoValidation_whenValidatorAbsent() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.PlainWidget",
            "package dev.verrai.processor;",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "import org.teavm.jso.dom.events.Event;",
            "public class PlainWidget implements IsWidget, TakesValue<String> {",
            "    public HTMLElement getElement() { return null; }",
            "    public void setValue(String value) {}",
            "    public String getValue() { return null; }",
            "    public void onBrowserEvent(Event e) {}",
            "}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.PlainModel",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Bindable;",
            "@Bindable",
            "public class PlainModel {",
            "    private String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.PlainPage",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import jakarta.inject.Inject;",
            "@Templated(\"SimplePage.html\")",
            "public class PlainPage {",
            "    @Inject @Model",
            "    public PlainModel model;",
            "    @Inject @DataField @Bound",
            "    public PlainWidget name;",
            "}"
        );

        // No validator source provided and model has no constraints → no _Validator generated
        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor(), new BindableProcessor())
            .compile(widget, model, page);

        assertThat(compilation).succeeded();

        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.PlainPage_Binder")
            .contentsAsUtf8String()
            .doesNotContain("validateField");
    }

    /**
     * When a @Bound field with an active validator is declared as an interface type (e.g. IsWidget)
     * AND the page does not implement ValidationAware, the processor emits a WARNING because
     * neither error-routing path will work at runtime.
     */
    @Test
    public void testValidationWarning_whenNeitherValidationAwareNorSetErrorMessage() {
        // Interface type — no setErrorMessage visible
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.IWidget",
            "package dev.verrai.processor;",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "public interface IWidget extends IsWidget, TakesValue<String> {}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.WarnModel",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Bindable;",
            "@Bindable",
            "public class WarnModel {",
            "    private String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        JavaFileObject validator = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.WarnModel_Validator",
            "package dev.verrai.processor;",
            "import dev.verrai.api.validation.ValidationResult;",
            "import dev.verrai.api.validation.Validator;",
            "public class WarnModel_Validator implements Validator<WarnModel> {",
            "    public static ValidationResult validateField(String f, Object v) {",
            "        return ValidationResult.valid();",
            "    }",
            "    public ValidationResult validate(WarnModel m) { return ValidationResult.valid(); }",
            "}"
        );

        // Page does NOT implement ValidationAware — warning should fire
        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.WarnPage",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import jakarta.inject.Inject;",
            "@Templated(\"SimplePage.html\")",
            "public class WarnPage {",
            "    @Inject @Model public WarnModel model;",
            "    @Inject @DataField @Bound public IWidget name;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(widget, model, validator, page);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("setErrorMessage");
        assertThat(compilation).hadWarningContaining("ValidationAware");
        // No setErrorMessage in generated binder — fallback is empty
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.WarnPage_Binder")
            .contentsAsUtf8String()
            .doesNotContain("setErrorMessage");
        // Validation still runs
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.WarnPage_Binder")
            .contentsAsUtf8String()
            .contains("validateField");
    }

    /**
     * When the page implements ValidationAware, no warning is emitted even if the widget
     * field is declared as an interface (IsWidget) with no setErrorMessage method.
     */
    @Test
    public void testNoWarning_whenPageImplementsValidationAware() {
        JavaFileObject widget = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.IWidget",
            "package dev.verrai.processor;",
            "import dev.verrai.api.IsWidget;",
            "import dev.verrai.api.TakesValue;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "public interface IWidget extends IsWidget, TakesValue<String> {}"
        );

        JavaFileObject model = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.AwareModel",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Bindable;",
            "@Bindable",
            "public class AwareModel {",
            "    private String name;",
            "    public String getName() { return name; }",
            "    public void setName(String v) { this.name = v; }",
            "}"
        );

        JavaFileObject validator = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.AwareModel_Validator",
            "package dev.verrai.processor;",
            "import dev.verrai.api.validation.ValidationResult;",
            "import dev.verrai.api.validation.Validator;",
            "public class AwareModel_Validator implements Validator<AwareModel> {",
            "    public static ValidationResult validateField(String f, Object v) {",
            "        return ValidationResult.valid();",
            "    }",
            "    public ValidationResult validate(AwareModel m) { return ValidationResult.valid(); }",
            "}"
        );

        // Page implements ValidationAware — no warning, binder calls onValidationResult
        JavaFileObject page = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.AwarePage",
            "package dev.verrai.processor;",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import dev.verrai.api.Model;",
            "import dev.verrai.api.Bound;",
            "import dev.verrai.api.validation.ValidationAware;",
            "import dev.verrai.api.validation.ValidationResult;",
            "import jakarta.inject.Inject;",
            "@Templated(\"SimplePage.html\")",
            "public class AwarePage implements ValidationAware {",
            "    @Inject @Model public AwareModel model;",
            "    @Inject @DataField @Bound public IWidget name;",
            "    @Override",
            "    public void onValidationResult(String field, ValidationResult result) {}",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(widget, model, validator, page);

        assertThat(compilation).succeeded();
        // No warning about suppressed validation — ValidationAware covers error routing
        for (javax.tools.Diagnostic<?> d : compilation.diagnostics()) {
            if (d.getKind() == javax.tools.Diagnostic.Kind.WARNING) {
                assertFalse("Unexpected validation warning: " + d.getMessage(java.util.Locale.ENGLISH),
                        d.getMessage(java.util.Locale.ENGLISH).contains("silently suppressed"));
            }
        }
        // Binder calls onValidationResult
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.AwarePage_Binder")
            .contentsAsUtf8String()
            .contains("onValidationResult");
        // Binder does NOT call setErrorMessage (no fallback needed)
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.AwarePage_Binder")
            .contentsAsUtf8String()
            .doesNotContain("setErrorMessage");
        // Model update only on valid
        assertThat(compilation)
            .generatedSourceFile("dev.verrai.processor.AwarePage_Binder")
            .contentsAsUtf8String()
            .contains("_vr.isValid()");
    }

    @Test
    public void testMissingDataFieldInTemplate() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "dev.verrai.processor.BrokenPage",
            "package dev.verrai.processor;",
            "",
            "import dev.verrai.api.Templated;",
            "import dev.verrai.api.DataField;",
            "import org.teavm.jso.dom.html.HTMLElement;",
            "",
            "@Templated(\"SimplePage.html\")",
            "public class BrokenPage {",
            "    @DataField",
            "    public HTMLElement missingField;",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new TemplatedProcessor())
            .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation)
            .hadErrorContaining("Template SimplePage.html does not contain data-field='missingField'");
    }
}
