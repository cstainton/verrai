# Verrai Developer Guide

This guide is for developers building applications with Verrai, and for contributors extending the framework itself. It covers the annotation processing pipeline end-to-end, the generated code patterns, and practical recipes for common tasks.

---

## Table of Contents

1. [How Verrai works](#how-verrai-works)
2. [The annotation processing pipeline](#the-annotation-processing-pipeline)
3. [Creating pages](#creating-pages)
4. [Navigation and routing](#navigation-and-routing)
5. [Dependency injection](#dependency-injection)
6. [Templating in depth](#templating-in-depth)
7. [Data binding](#data-binding)
8. [Form validation](#form-validation)
9. [Events](#events)
10. [Security](#security)
11. [Widgets](#widgets)
12. [Extending the framework](#extending-the-framework)
13. [Testing processors](#testing-processors)
14. [Build reference](#build-reference)

---

## How Verrai works

Verrai runs entirely in the browser. There is no server-side runtime. Here is the full compilation pipeline:

```
Java source
    │
    ▼
javac + annotation processors (APT)
    │  TemplatedProcessor   → *_Binder.java
    │  BindableProcessor    → *_BindableProxy.java
    │  IOCProcessor         → *_Factory.java
    │  NavigationProcessor  → NavigationImpl.java
    ▼
javac compiles generated sources alongside app sources
    │
    ▼
TeaVM compiler
    │  Compiles Java bytecode → JavaScript (classes.js)
    │  Dead-code eliminates anything not reachable from main()
    ▼
Browser loads index.html → classes.js → main() → NavigationImpl.start()
```

**The key constraint:** TeaVM has no runtime reflection. Every factory, binder, and proxy must be generated at compile time. The processors do this by reading source-level annotations and type information through the standard `javax.annotation.processing` API, then writing Java source files using JavaPoet.

---

## The annotation processing pipeline

Four processors run in a single `javac` invocation, each with a distinct responsibility:

### IOCProcessor

Scans all `@ApplicationScoped` and `@Dependent` beans. For each, generates a `ClassName_Factory.java`:

```java
// Generated: MyService_Factory.java
public class MyService_Factory {
    private static MyService instance;

    public static MyService getInstance() {
        if (instance == null) instance = createInstance();
        return instance;
    }

    private static MyService createInstance() {
        MyService bean = new MyService();
        bean.dependency = OtherService_Factory.getInstance();
        bean.init();  // @PostConstruct
        return bean;
    }
}
```

`@ApplicationScoped` beans use a static `instance` field (lazy singleton). `@Dependent` beans call `createInstance()` directly each time.

The processor builds a **resolution map** during the `PROCESS` phase: interface → implementing class, qualified by `@Named` if present. When a field of type `MyInterface` is injected, the map determines which concrete `*_Factory` to call.

**Cross-module injection** works because factories are regular Java classes on the classpath. The processor only needs the factory to exist at link time, not at its own compile time.

### BindableProcessor

Scans all `@Bindable` classes. Generates `ClassName_BindableProxy.java` that extends the model class:

```java
// Generated: UserModel_BindableProxy.java
public class UserModel_BindableProxy extends UserModel implements BindableProxy<UserModel> {
    private List<PropertyChangeHandler> handlers = new ArrayList<>();

    @Override
    public void setName(String name) {
        super.setName(name);
        firePropertyChange("name", name);
    }

    private void firePropertyChange(String property, Object value) {
        for (PropertyChangeHandler h : handlers) h.onPropertyChange(property, value);
    }

    @Override public Subscription addPropertyChangeHandler(PropertyChangeHandler h) {
        handlers.add(h);
        return () -> handlers.remove(h);
    }
}
```

Every setter is overridden to call the original and then fire the change event. This is what makes `model.setName("Alice")` automatically update any bound widget.

### TemplatedProcessor

Scans all `@Templated` classes. For each:

1. Locates the matching HTML template (same package, same simple name, `.html` extension, or overridden by `@Templated("path/to/file.html")`)
2. Parses the HTML with JSoup
3. Validates that every `@DataField` has a matching `data-field` attribute in the template
4. Generates `ClassName_Binder.java` with a static `bind(ClassName target)` method

The binder:
- Queries the DOM for each `data-field` element
- If the field is a `Widget`/`IsWidget`: replaces the placeholder element with the widget's element
- If the field is `HTMLElement`: stores the reference
- Registers `@EventHandler` methods as DOM event listeners
- Registers `@SinkNative` event sinks
- Wires two-way data binding for `@Bound` fields
- Calls any `@Observes` registrations
- Handles `@RestrictedAccess` by removing elements for unauthorized users

### NavigationProcessor

Scans all `@Page` classes. Generates a single `NavigationImpl.java` that:

- Maintains a `currentPage` reference and `previousRole` for back-navigation
- Parses the URL hash on `start()` and navigates to the correct page
- Implements a `goTo(role, state)` method with a `switch` over all known roles
- Calls `@PageHiding`/`@PageHidden` on the outgoing page
- Calls `@PageShowing` on the incoming page, passing `@PageState` fields from the state map
- Checks `CanDeactivate` before leaving, `CanActivate` before entering
- Wraps key sections in `ErrorBus.dispatch(...)` calls for runtime error handling

---

## Creating pages

A page is any class annotated with `@Page`. The `role` string becomes the URL hash fragment.

```java
@Page(role = "settings")
@Templated
public class SettingsPage {

    @Inject @DataField Button saveBtn;
    @Inject @DataField TextBox usernameBox;
    @Inject Navigation navigation;

    @PageState String section;  // populated from navigation state

    @PageShowing
    public void onShow() {
        if ("security".equals(section)) showSecurityTab();
    }

    @PageHiding
    public void onLeave() {
        // save draft, etc.
    }

    @EventHandler("saveBtn")
    public void onSave() {
        navigation.goTo("dashboard");
    }
}
```

### Lifecycle methods

| Annotation | When called |
|---|---|
| `@PageShowing` | After the page is mounted in the DOM, before the user sees it |
| `@PageHiding` | Before the page is removed from the DOM |
| `@PageHidden` | After the page has been removed |

### Receiving navigation state

```java
navigation.goTo("settings", Map.of("section", "security"));

// In SettingsPage:
@PageState String section;  // "security"
```

State values are always `String`. The field is set before `@PageShowing` is called.

### Starting page

```java
@Page(role = "login", startingPage = true)
```

Only one page may be `startingPage = true`. Navigation falls back to it on unknown routes or after errors.

---

## Navigation and routing

Navigation is hash-based: `https://example.com/#dashboard`. The `Navigation` interface is generated and injected by the framework.

```java
navigation.goTo("dashboard");
navigation.goTo("profile", Map.of("userId", userId));
```

### Route guards

**`CanDeactivate`** — checked before leaving the current page:

```java
public class EditPage implements CanDeactivate {
    private boolean dirty;

    @Override
    public boolean canDeactivate() {
        if (dirty) {
            return Window.confirm("Discard unsaved changes?");
        }
        return true;
    }
}
```

**`CanActivate`** — checked before entering the target page. Receives the target role and state:

```java
public class AdminPage implements CanActivate {
    @Inject SecurityProvider security;

    @Override
    public boolean canActivate(String toRole, Map<String, String> state) {
        return security.hasRole("ADMIN");
    }
}
```

If `canActivate` returns `false`, navigation rolls back to the previous page.

### Listening to navigation events

```java
navigation.addListener(new NavigationListener() {
    @Override public void onNavigate(String fromRole, String toRole) { ... }
});
```

---

## Dependency injection

### Scopes

| Annotation | Behaviour |
|---|---|
| `@ApplicationScoped` | One instance for the lifetime of the application |
| `@Dependent` | New instance per injection point |

### Injection targets

Fields annotated `@Inject` are populated by the factory. Fields must be `public` or package-private (the factory is in the same package).

```java
@Inject MyService service;
@Inject Provider<HeavyThing> heavy;  // lazy — creates on first get()
@Inject Event<MyEvent> myEvent;
@Inject Navigation navigation;
```

### Qualifiers

Use `@Named` to distinguish multiple implementations of the same interface:

```java
@ApplicationScoped @Named("primary")
public class PrimaryRepo implements UserRepository { ... }

@ApplicationScoped @Named("cache")
public class CachingRepo implements UserRepository { ... }

// In consumer:
@Inject @Named("primary") UserRepository repo;
```

### `@PostConstruct`

Methods annotated `@PostConstruct` are called by the factory after all fields are injected:

```java
@ApplicationScoped
public class MyService {
    @Inject OtherService other;

    @PostConstruct
    public void init() {
        // other is already injected here
    }
}
```

### Cross-module injection

Beans defined in library JARs work out of the box, because their `*_Factory` classes are already compiled and on the classpath. The processor discovers them via the resolution map built from annotations visible at compile time.

**Important:** Any class you inject that is not `@ApplicationScoped` or `@Dependent` will generate a compile-time warning, because no factory will be generated for it, and injection will fail at runtime with a `ClassNotFoundException`.

---

## Templating in depth

### Template resolution

The processor looks for the template file in the same package as the Java class, in `src/main/resources`. For `com.example.LoginPage`, it expects `com/example/LoginPage.html`.

Override the path with `@Templated("path/relative/to/package/other.html")`.

### `@DataField`

The simplest case: field name matches `data-field` attribute.

```java
@Inject @DataField Button submitBtn;
// matches: <button data-field="submitBtn">
```

Override the attribute name:

```java
@Inject @DataField("submit-button") Button btn;
// matches: <button data-field="submit-button">
```

**Widget fields** (those implementing `IsWidget`) have their HTML element substituted for the placeholder element. The placeholder is replaced in the DOM, inheriting any CSS classes you put on it:

```html
<button data-field="submitBtn" class="btn btn-primary">Submit</button>
```

The button widget's internal `<button>` element replaces the placeholder. The `class` attribute on the placeholder is ignored after replacement — style the widget programmatically or via your widget's constructor.

**`HTMLElement` fields** receive a reference to the actual DOM node. Use these when you need direct DOM manipulation:

```java
HTMLElement headerEl;  // no @Inject needed for raw elements
@DataField HTMLDivElement panel;
```

### `@RootElement`

Gets a reference to the page's root HTML element (the outermost element in the template):

```java
@RootElement HTMLElement root;
```

### Event handling

```java
@EventHandler("submitBtn")              // click by default
public void onSubmit() { ... }

@EventHandler(value = "nameBox", type = "input")
public void onNameInput() { ... }
```

The processor validates that the named field exists. The method may optionally take the native `Event` parameter.

### `@SinkNative`

Attach native browser events to a widget field, delegating to `widget.onBrowserEvent(event)`:

```java
@Inject @DataField @SinkNative({"click", "mouseover"}) MyWidget widget;
```

The widget must override `onBrowserEvent(Event)`.

---

## Data binding

### The model

A `@Bindable` model class has standard getters/setters. Nested models are also `@Bindable`:

```java
@Bindable
public class OrderModel {
    private String notes;
    private Address shippingAddress;  // @Bindable Address

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    // ... address getter/setter
}
```

### The page

```java
@Model OrderModel model;               // declares the model instance

@Inject @DataField @Bound TextBox notesField;          // → model.notes
@Inject @DataField @Bound TextBox streetField;         // → model.shippingAddress.street
```

The `@Bound` property path defaults to the field name. For nested properties, the path is `parentField.childProperty`:

```java
@Inject @DataField("streetField") @Bound(property = "shippingAddress.street") TextBox sf;
```

### Binding lifecycle

The generated `*_Binder` registers `PropertyChangeHandler`s on the model proxy. These are `Subscription` objects. If you navigate away without clearing them, the handlers fire on a detached page.

Implement `BinderLifecycle` to let the binder register and clean up subscriptions automatically:

```java
@Page(role = "order")
@Templated
public class OrderPage implements BinderLifecycle {
    private final List<Subscription> bindings = new ArrayList<>();

    @Override public void addBinding(Subscription s) { bindings.add(s); }
    @Override public void clearBindings() {
        bindings.forEach(Subscription::remove);
        bindings.clear();
    }

    @PageHiding
    public void onLeave() {
        clearBindings();
    }
}
```

### Converters

When a model field is not `String`, register a `Converter<Model, Widget>`. Built-in converters are registered for `Integer`, `Long`, `Double`, and `Boolean` → `String`. Register custom ones at startup:

```java
ConverterRegistry.register(LocalDate.class, String.class,
    new Converter<LocalDate, String>() {
        public String toWidget(LocalDate d) { return d.toString(); }
        public LocalDate toModel(String s) { return LocalDate.parse(s); }
    });
```

---

## Form validation

### Constraint annotations

Apply constraints to `@Bindable` model fields:

```java
@Bindable
public class SignupModel {
    @NotEmpty
    @Size(min = 2, max = 50, message = "Name must be 2–50 characters")
    private String name;

    @Pattern(regexp = ".+@.+\\..+", message = "Enter a valid email")
    private String email;

    @Min(value = 0)
    @Max(value = 150)
    private int age;
}
```

| Annotation | Validates |
|---|---|
| `@NotNull` | Value is not null |
| `@NotEmpty` | String is not null or empty |
| `@Size(min, max)` | String length within bounds |
| `@Min(value)` | Numeric value ≥ threshold |
| `@Max(value)` | Numeric value ≤ threshold |
| `@Pattern(regexp)` | String matches regular expression |

### Generated validator

The `BindableProcessor` generates `SignupModel_Validator` with a static `validateField(String fieldName, Object value)` method. The binder calls this on every widget change event before updating the model.

### Routing validation results

**Preferred: implement `ValidationAware`**

The binder calls `onValidationResult(fieldName, result)` on your page for every field change. This gives you full control over how errors are displayed:

```java
@Page(role = "signup")
@Templated
public class SignupPage implements ValidationAware {

    @Inject @DataField TextBox nameField;  // IsWidget — binder can't directly call setErrorMessage
    @Inject @DataField TextBox emailField;

    @Override
    public void onValidationResult(String field, ValidationResult result) {
        switch (field) {
            case "name":
                if (result.isValid()) nameField.clearErrorMessage();
                else nameField.setErrorMessage(result.getMessages().get(0));
                break;
            case "email":
                if (result.isValid()) emailField.clearErrorMessage();
                else emailField.setErrorMessage(result.getMessages().get(0));
                break;
        }
    }
}
```

`ValidationAware` is also the recommended path for surfacing **server-side validation errors**:

```java
service.register(request, response -> {
    for (FieldError e : response.getErrors()) {
        onValidationResult(e.getField(), ValidationResult.invalid(e.getMessage()));
    }
});
```

**Fallback: duck-typing**

If the page does not implement `ValidationAware` and the bound field's concrete type exposes `setErrorMessage(String)` and `clearErrorMessage()`, the binder calls those methods directly. Any widget class with those two methods works — no base class required.

If neither path is available, the processor emits a compile-time warning. Invalid values are still blocked from reaching the model.

---

## Events

### Firing events

```java
@Inject Event<OrderPlacedEvent> orderEvent;

orderEvent.fire(new OrderPlacedEvent(orderId));
```

### Observing events

Any `@ApplicationScoped` or `@Page` bean can observe events by declaring a method with an `@Observes` parameter:

```java
@ApplicationScoped
public class AuditService {
    public void onOrder(@Observes OrderPlacedEvent event) {
        log("Order placed: " + event.getOrderId());
    }
}
```

The `IOCProcessor` registers the observer in the factory's `createInstance()` method via `EventBus.register(...)`.

### Error handling

`ErrorBus` provides a global escape hatch for unhandled exceptions in navigation and lifecycle calls:

```java
ErrorBus.register((context, throwable) -> {
    Window.alert("Unexpected error in " + context + ": " + throwable.getMessage());
});
```

Register your handler early — e.g., in the `main()` method or a `@PostConstruct` method on a startup bean.

---

## Security

### Restricting pages

```java
@Page(role = "admin")
@RestrictedAccess(roles = {"ADMIN"})
@Templated
public class AdminPage { ... }
```

If the current user lacks the required role, navigation to this page is blocked.

### Restricting fields

Individual `@DataField` widgets can also be access-controlled. The templated processor removes the element from the DOM entirely for unauthorised users:

```java
@Inject @DataField @RestrictedAccess(roles = {"ADMIN"}) Button deleteBtn;
```

### Providing roles

Implement `SecurityProvider` and annotate it `@ApplicationScoped`:

```java
@ApplicationScoped
public class AppSecurity implements SecurityProvider {
    private Set<String> currentRoles = new HashSet<>();

    public void setRoles(Set<String> roles) { this.currentRoles = roles; }

    @Override
    public boolean hasRole(String role) { return currentRoles.contains(role); }
}
```

The processor detects your implementation via the interface and wires it into the generated `NavigationImpl`.

---

## Widgets

### Using Bootstrap widgets

```java
@Inject @DataField Button submit;       // auto-injected and wired to template
```

Or create programmatically for dynamic content:

```java
Button btn = new Button();
btn.setText("Click me");
btn.setType(Button.Type.PRIMARY);
btn.addClickListener(e -> doSomething());
someContainer.getElement().appendChild(btn.getElement());
```

### Error display

All Bootstrap and Material widgets expose:

```java
widget.setErrorMessage("This field is required");
widget.clearErrorMessage();
```

Bootstrap widgets add Bootstrap's `is-invalid` class and a `<div class="invalid-feedback">` sibling. Material widgets use the MDC helper-text pattern.

### `TableWidget`

The table supports sorting, filtering, and pagination entirely in-browser:

```java
@Inject @DataField TableWidget table;

@PageShowing
public void onShow() {
    table.setHeaders("Name", "Email", "Role");
    table.setSortable(true);
    table.setFilterable(true);
    table.setPageSize(10);

    for (User u : userList) {
        table.addRow(u.getName(), u.getEmail(), u.getRole());
    }

    // Mixed widget row:
    Button edit = new Button(); edit.setText("Edit");
    Button del = new Button(); del.setText("Delete"); del.setType(Button.Type.DANGER);
    table.addWidgetRow(edit, del);
}
```

### Writing a custom widget

Widgets are plain Java classes. Use `@Dependent` so the IOC processor generates a factory for them:

```java
@Dependent
public class StarRating extends Widget {

    private int stars = 0;

    public StarRating() {
        super(DOMHelper.createDiv("star-rating"));
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            HTMLSpanElement star = DOMHelper.createSpan("star");
            star.setTextContent("★");
            star.addEventListener("click", e -> setStars(n));
            element.appendChild(star);
        }
    }

    public int getStars() { return stars; }

    public void setStars(int stars) {
        this.stars = stars;
        // update styling
    }
}
```

Use it exactly like any built-in widget — `@Inject @DataField StarRating rating` — as long as it is on the classpath of the module being compiled.

---

## Extending the framework

### Adding a new annotation

1. Define the annotation in `verrai-client-api` with appropriate `@Target` and `@Retention(RUNTIME)`.
2. Add a handler in the relevant processor (or create a new processor class).
3. Register the new processor in `verrai-client-annotation-processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor`.

### Adding to an existing processor

All processors follow the **Visitor pattern** internally. For example, `TemplatedProcessor` delegates to `BinderWriter`, which visits `BeanDefinition` objects. To add new generated code, modify the appropriate visitor or add a new one.

Key model classes:

| Class | Location | Description |
|---|---|---|
| `BeanDefinition` | `processor/model/` | Everything the processor knows about one bean |
| `InjectionPoint` | `processor/model/` | One `@Inject` field |
| `PageDefinition` | `processor/model/` | A `@Page` class with lifecycle and guard info |
| `FactoryWriter` | `processor/visitor/` | Generates `*_Factory.java` |
| `NavigationImplWriter` | `processor/visitor/` | Generates `NavigationImpl.java` |
| `BinderWriter` | `processor/visitor/` | Generates `*_Binder.java` |

The processors use **JavaPoet** (`TypeSpec`, `MethodSpec`, `FieldSpec`) for code generation. Prefer JavaPoet's type-safe builders over string concatenation.

### Cross-round compilation

APT may run in multiple rounds. The processors use `roundEnv.processingOver()` to defer final output until all annotations have been collected. Data accumulated during `process()` rounds is stored in instance fields of the processor.

---

## Testing processors

The test suite uses **Google compile-testing** (`com.google.testing.compile:compile-testing`) to compile in-memory source files and assert on the results:

```java
@Test
public void testFactoryGenerated() {
    JavaFileObject source = JavaFileObjects.forSourceLines(
        "com.example.MyService",
        "package com.example;",
        "import jakarta.enterprise.context.ApplicationScoped;",
        "@ApplicationScoped",
        "public class MyService {}"
    );

    Compilation compilation = javac()
        .withProcessors(new IOCProcessor())
        .compile(source);

    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("com.example.MyService_Factory");
    assertThat(compilation)
        .generatedSourceFile("com.example.MyService_Factory")
        .contentsAsUtf8String()
        .contains("getInstance()");
}
```

**Warnings:** use raw diagnostics instead of `hadWarningCount(0)` — the `javac` processor always emits a harmless `RELEASE_11` compatibility warning:

```java
for (javax.tools.Diagnostic<?> d : compilation.diagnostics()) {
    if (d.getKind() == WARNING) {
        assertFalse("Unexpected warning: " + d.getMessage(Locale.ENGLISH),
            d.getMessage(Locale.ENGLISH).contains("text to reject"));
    }
}
```

Run the processor tests:

```bash
./mvnw -pl verrai-client-processor test
```

---

## Build reference

### Module dependency order

```
verrai-client-annotation-processor
verrai-client-api
verrai-client-security
verrai-client-processor        (depends on verrai-client-api)
verrai-client-widgets-bootstrap (depends on verrai-client-api)
verrai-client-widgets-material  (depends on verrai-client-api)
verrai-client-app               (depends on all of the above)
```

### Common commands

```bash
# Test processors only (fast feedback loop during processor development)
./mvnw -pl verrai-client-processor test

# Build the full client chain (skipping tests)
./mvnw -pl verrai-client-annotation-processor,verrai-client-api,verrai-client-security,\
       verrai-client-processor,verrai-client-app -am -DskipTests clean install

# Build + test everything
./mvnw -pl verrai-client-annotation-processor,verrai-client-api,verrai-client-security,\
       verrai-client-processor,verrai-client-app -am clean install

# Serve the demo app locally
cd verrai-client-app/target/verrai-client-app-1.0-SNAPSHOT-gh-pages
python3 -m http.server 8080
```

### Adding a new module as a dependency

If your new module contains widgets or services that other modules inject, annotate the beans with `@Dependent` or `@ApplicationScoped`. The IOC processor in the consuming module will detect the annotations on the classpath and generate the correct factory call, but it will NOT re-run the provider module's processor — those factories must already be compiled and present.

This is why `verrai-client-widgets-bootstrap` and `verrai-client-widgets-material` are compiled as separate library JARs before `verrai-client-app` is built.
