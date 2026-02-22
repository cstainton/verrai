# Verrai — TeaVM SPA Framework

**Verrai** is a framework for building Single Page Applications (SPAs) in pure Java, compiled to JavaScript by [TeaVM](https://teavm.org/). It brings familiar CDI/Errai-style patterns — dependency injection, declarative templating, two-way data binding, and client-side navigation — to a lean, reflection-free compile-time model that runs in any browser without a JVM.

**[Live Demo](https://cstainton.github.io/verrai/)** — deployed automatically from `main` via GitHub Actions.

> **Status:** Proof of Concept. The architecture is stable and the feature set is broad, but APIs may change before a 1.0 release.

---

## What it does

| Feature | How |
|---|---|
| **Dependency Injection** | `@ApplicationScoped` / `@Dependent` — factories generated at compile time, no reflection |
| **Declarative Templating** | `@Templated` + `@DataField` — HTML wired to Java fields by the processor |
| **Two-way Data Binding** | `@Bindable` / `@Bound` / `@Model` — model changes propagate to widgets and vice versa |
| **Client-side Routing** | `@Page` + `Navigation` — hash-based SPA routing with lifecycle hooks and route guards |
| **Form Validation** | `@NotNull`, `@NotEmpty`, `@Size`, `@Pattern`, `@Min`, `@Max` — inline at binding time |
| **Widget Libraries** | Bootstrap 5 and Material Design component sets, both usable side-by-side |
| **Events** | CDI-style `@Observes` / `Event<T>` for loose coupling between pages |
| **Security** | `@RestrictedAccess(roles=…)` on pages and fields, backed by a `SecurityProvider` |
| **i18n** | `@Bundle` + `.properties` files — type-safe message lookup, no runtime resource loading |

---

## Quick Start

### 1. Add the dependencies

```xml
<!-- In your client module's pom.xml -->
<dependency>
    <groupId>dev.verrai</groupId>
    <artifactId>verrai-client-api</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>dev.verrai</groupId>
    <artifactId>verrai-client-widgets-bootstrap</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<!-- Annotation processor (compile scope only) -->
<dependency>
    <groupId>dev.verrai</groupId>
    <artifactId>verrai-client-annotation-processor</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Configure the TeaVM Maven plugin to compile your module to JavaScript (see `verrai-client-app/pom.xml` for a full example).

### 2. Write a page

```java
// src/main/java/com/example/GreetingPage.java
@Page(role = "greeting", startingPage = true)
@Templated
public class GreetingPage {

    @Inject @DataField Button greetBtn;
    @Inject @DataField TextBox nameBox;
    @Inject Navigation navigation;

    @PageShowing
    public void onShow() {
        greetBtn.setText("Say Hello");
    }

    @EventHandler("greetBtn")
    public void onGreet() {
        Window.alert("Hello, " + nameBox.getValue() + "!");
    }
}
```

```html
<!-- src/main/resources/com/example/GreetingPage.html -->
<div class="container mt-4">
  <input data-field="nameBox" class="form-control mb-2" placeholder="Your name">
  <button data-field="greetBtn" class="btn btn-primary"></button>
</div>
```

### 3. Build

```bash
cd verrai
./mvnw -pl verrai-client-annotation-processor,verrai-client-api,verrai-client-security,\
       verrai-client-processor,verrai-client-app -am clean package
```

The TeaVM-compiled output lands at `verrai-client-app/target/generated/teavm/classes.js`.

---

## Core Concepts

### Pages and routing

Every page is a Java class annotated with `@Page(role = "my-page")`. The generated `NavigationImpl` maps URL hashes (`#my-page`) to page classes.

```java
@Page(role = "dashboard")
@Templated
public class DashboardPage {

    @PageShowing
    public void onShow() { /* called when navigated to */ }

    @PageHiding
    public void onLeave() { /* called before navigating away */ }
}
```

Navigate programmatically:

```java
@Inject Navigation navigation;

navigation.goTo("dashboard");
navigation.goTo("profile", Map.of("userId", "42"));
```

Receive state on the target page:

```java
@PageState String userId;  // populated from state map
```

### Templating

`@Templated` binds a Java class to an HTML file in the same package. Fields annotated `@DataField` are matched to elements with a `data-field` attribute of the same name.

```java
@Inject @DataField Button submitBtn;   // matches <button data-field="submitBtn">
@Inject @DataField TextBox emailBox;   // matches <input data-field="emailBox">
```

Widget fields replace the placeholder element entirely; `HTMLElement` fields receive a reference to the DOM node.

### Dependency injection

Beans are `@ApplicationScoped` (singleton per session) or `@Dependent` (new instance per injection point). The processor generates a `*_Factory` class for each bean — no runtime reflection involved.

```java
@ApplicationScoped
public class UserService {
    public String getCurrentUser() { return "alice"; }
}

@Page(role = "profile")
@Templated
public class ProfilePage {
    @Inject UserService userService;   // injected via generated factory
}
```

Inject a lazy `Provider<T>` for on-demand instantiation:

```java
@Inject Provider<HeavyService> heavyService;  // obtained as a lambda
```

### Data binding

Annotate your model with `@Bindable` — the processor generates a proxy that fires change events:

```java
@Bindable
public class UserModel {
    private String name;
    private Address address;  // Address is also @Bindable

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... address getter/setter
}
```

In the page, declare the model field and bind it to widgets:

```java
@Model UserModel model;

@Inject @DataField @Bound TextBox nameField;          // binds to model.name
@Inject @DataField @Bound TextBox streetField;        // binds to model.address.street
```

The generated binder wires change listeners in both directions. When `model.setName("Alice")` is called, `nameField` updates automatically. When the user types, the model updates.

To clean up listeners when navigating away, implement `BinderLifecycle`:

```java
public class ProfilePage implements BinderLifecycle {
    private final List<Subscription> bindings = new ArrayList<>();
    @Override public void addBinding(Subscription s) { bindings.add(s); }
    @Override public void clearBindings() { bindings.forEach(Subscription::remove); bindings.clear(); }
}
```

### Form validation

Add constraint annotations to model fields:

```java
@Bindable
public class RegistrationModel {
    @NotEmpty(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;

    @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]+$", message = "Invalid email")
    private String email;

    @Min(value = 18, message = "Must be 18 or older")
    private int age;
}
```

The processor generates a `RegistrationModel_Validator` class. When a bound widget changes, the binder calls `validateField` and:

- If the page implements `ValidationAware`, routes the result there (recommended):

```java
public class RegistrationPage implements ValidationAware {
    @Override
    public void onValidationResult(String field, ValidationResult result) {
        if (!result.isValid()) {
            nameField.setErrorMessage(result.getMessages().get(0));
        } else {
            nameField.clearErrorMessage();
        }
    }
}
```

- Otherwise, falls back to duck-typing: calls `setErrorMessage`/`clearErrorMessage` directly on the widget if those methods exist.

### Event bus

Fire and observe events between loosely coupled pages:

```java
// Producer
@Inject Event<UserLoggedInEvent> loginEvent;
loginEvent.fire(new UserLoggedInEvent("alice"));

// Observer (any @ApplicationScoped or @Page bean)
public void onLogin(@Observes UserLoggedInEvent event) {
    navbar.setUser(event.getUsername());
}
```

### Route guards

Prevent leaving a page (e.g., unsaved changes):

```java
public class EditPage implements CanDeactivate {
    @Override
    public boolean canDeactivate() {
        return !hasUnsavedChanges();
    }
}
```

Prevent entering a page (e.g., auth check):

```java
public class AdminPage implements CanActivate {
    @Override
    public boolean canActivate(String toRole, Map<String, String> state) {
        return securityProvider.hasRole("ADMIN");
    }
}
```

### Security

Restrict page access at the routing level:

```java
@Page(role = "admin")
@RestrictedAccess(roles = {"ADMIN"})
@Templated
public class AdminPage { ... }
```

Or restrict individual widgets within a page:

```java
@Inject @DataField @RestrictedAccess(roles = {"ADMIN"}) Button deleteBtn;
```

Implement `SecurityProvider` to plug in your auth logic:

```java
@ApplicationScoped
public class MySecurityProvider implements SecurityProvider {
    @Override
    public boolean hasRole(String role) {
        return Session.getRoles().contains(role);
    }
}
```

---

## Widget Libraries

### Bootstrap 5 (`verrai-client-widgets-bootstrap`)

All widgets are `@Dependent` and implement `IsWidget`. Key components:

| Widget | Description |
|---|---|
| `Button` | Typed (Primary/Secondary/…), sized, outline, disabled |
| `TextBox` | Input with listeners for `input` (each keystroke) and `change` |
| `TextArea` | Multi-line text input |
| `Select` | Dropdown; `addOption()`, `clearOptions()` |
| `Checkbox`, `Switch`, `RadioButton`, `Slider` | Form controls with `addChangeHandler()` |
| `Card`, `Alert`, `Badge`, `Modal` | Content containers |
| `Navbar`, `Navs`, `Breadcrumb` | Navigation chrome |
| `TableWidget` | Sortable, filterable, paginated data table |
| `Accordion`, `Tooltip`, `Popover` | Overlay components |
| `ProgressBar`, `Spinner`, `Placeholder` | Feedback widgets |

All widgets expose `setErrorMessage(String)` / `clearErrorMessage()` for validation display.

### Material Design (`verrai-client-widgets-material`)

| Widget | Description |
|---|---|
| `MaterialButton` | MDC button with icon and ripple |
| `MaterialTextField` | MDC outlined text field |
| `MaterialCard` | Title, subtitle, body text, actions |
| `MaterialDialog` | Modal dialog; `open()` / `close()` |
| `MaterialDrawer` | Side navigation drawer |
| `MaterialList` | MDC list with icons and click handlers |
| `MaterialFAB` | Floating action button |
| `MaterialMenu` | Anchored dropdown menu |
| `MaterialChip` | Filterable chip with icon |
| `MaterialProgress` | Linear progress bar (0.0–1.0 or indeterminate) |
| `MaterialAlert` | Snackbar-style transient notification |

---

## Project Structure

```
verrai/
├── verrai-client-api/              Core annotations and interfaces
├── verrai-client-processor/        Annotation processors (the framework engine)
├── verrai-client-annotation-processor/  Service descriptor for Maven APT
├── verrai-client-widgets-bootstrap/    Bootstrap 5 widget library
├── verrai-client-widgets-material/     Material Design widget library
├── verrai-client-ui/               Base composites and shared utilities
├── verrai-client-security/         Role-based access control
├── verrai-client-persistence/      Entity management
└── verrai-client-app/              Sample application (live demo source)
```

---

## Building and Running Locally

Prerequisites: JDK 21, Maven 3.8+.

```bash
# Clone and build the framework + sample app
git clone https://github.com/cstainton/verrai.git
cd verrai/verrai

./mvnw -pl verrai-client-annotation-processor,verrai-client-api,verrai-client-security,\
       verrai-client-processor,verrai-client-app -am clean package

# Serve the app locally (any static server)
cd verrai-client-app/target/verrai-client-app-1.0-SNAPSHOT-gh-pages
python3 -m http.server 8080
# Open http://localhost:8080
```

---

## Further Reading

- **[Developer Guide](DEVELOPER.md)** — how the processors work, how to add new pages/widgets, how to extend the framework
- **[Data Binding Guide](BINDING.md)** — deep dive on @Bindable, property paths, converters
- **[Comparison with Errai](COMPARISON.md)** — design decisions and trade-offs vs. the original Errai framework

---

## License

This project is a Proof of Concept and serves as a technical demonstration.
