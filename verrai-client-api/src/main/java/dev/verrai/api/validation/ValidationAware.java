package dev.verrai.api.validation;

/**
 * Implemented by pages that want to own validation error routing.
 *
 * <p>When a {@code @Bound} field has an active validator and the page implements this
 * interface, the generated binder calls {@link #onValidationResult} on every change
 * event instead of writing directly to the widget.  This lets the page decide how to
 * surface errors — inline on the widget, in a summary panel, by gating a submit button,
 * or any combination.
 *
 * <p>Server-side validation results can be fed through the same method by calling it
 * directly from an async callback, giving a single error-handling path regardless of
 * where validation originates.
 *
 * <p>Pages that do <em>not</em> implement this interface fall back to a duck-typed
 * direct call: if the bound widget's concrete type exposes {@code setErrorMessage(String)}
 * and {@code clearErrorMessage()}, those are called automatically.  A compile-time
 * warning is emitted when neither path is available.
 *
 * <pre>{@code
 * @Page(role = "registration")
 * @Templated
 * public class RegistrationPage implements ValidationAware {
 *
 *     @Inject @DataField @Bound TextBox email;
 *
 *     // Called by the binder on every keystroke; also call directly for server errors.
 *     @Override
 *     public void onValidationResult(String field, ValidationResult result) {
 *         switch (field) {
 *             case "email":
 *                 if (result.isValid()) email.clearErrorMessage();
 *                 else                  email.setErrorMessage(result.getMessages().get(0));
 *                 break;
 *         }
 *     }
 *
 *     @EventHandler("submitBtn")
 *     public void onSubmit() {
 *         service.register(model, response -> {
 *             for (FieldError e : response.getErrors()) {
 *                 onValidationResult(e.getField(), ValidationResult.invalid(e.getMessage()));
 *             }
 *         });
 *     }
 * }
 * }</pre>
 */
public interface ValidationAware {

    /**
     * Called whenever a bound field's value changes and a validator is active for it,
     * or directly by application code to surface server-side validation results.
     *
     * @param fieldName the model property name (matches the {@code @Bound} property path)
     * @param result    the validation result; check {@link ValidationResult#isValid()} first
     */
    void onValidationResult(String fieldName, ValidationResult result);
}
