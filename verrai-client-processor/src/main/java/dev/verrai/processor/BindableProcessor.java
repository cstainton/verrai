package dev.verrai.processor;

import dev.verrai.api.Bindable;
import dev.verrai.api.binding.BindableProxy;
import dev.verrai.api.binding.PropertyChangeHandler;
import dev.verrai.api.binding.Subscription;
import dev.verrai.api.validation.NotNull;
import dev.verrai.api.validation.NotEmpty;
import dev.verrai.api.validation.Size;
import dev.verrai.api.validation.Min;
import dev.verrai.api.validation.Max;
import dev.verrai.api.validation.Pattern;
import com.squareup.javapoet.*;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("dev.verrai.api.Bindable")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class BindableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Bindable.class)) {
            if (element.getKind() != ElementKind.CLASS) continue;
            try {
                processType((TypeElement) element);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error processing @Bindable: " + e.getMessage(), element);
                e.printStackTrace();
            }
        }
        return true;
    }

    private void processType(TypeElement typeElement) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String proxyName = typeElement.getSimpleName() + "_BindableProxy";
        ClassName typeName = ClassName.get(typeElement);
        ClassName proxyClassName = ClassName.get(packageName, proxyName);
        ClassName handlerClass = ClassName.get(PropertyChangeHandler.class);
        ClassName listClass = ClassName.get(List.class);
        ClassName arrayListClass = ClassName.get(ArrayList.class);

        TypeSpec.Builder proxyBuilder = TypeSpec.classBuilder(proxyName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(typeName)
                .addSuperinterface(BindableProxy.class);

        // Field: List<PropertyChangeHandler> handlers
        proxyBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(listClass, handlerClass), "handlers")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", arrayListClass)
                .build());

        // Method: addPropertyChangeHandler
        proxyBuilder.addMethod(MethodSpec.methodBuilder("addPropertyChangeHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(Subscription.class)
                .addParameter(handlerClass, "handler")
                .addStatement("this.handlers.add(handler)")
                .addStatement("return () -> this.handlers.remove(handler)")
                .build());

        // Method: removePropertyChangeHandler
        proxyBuilder.addMethod(MethodSpec.methodBuilder("removePropertyChangeHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(handlerClass, "handler")
                .addStatement("this.handlers.remove(handler)")
                .build());

        // Method: firePropertyChange
        proxyBuilder.addMethod(MethodSpec.methodBuilder("firePropertyChange")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "property")
                .addParameter(Object.class, "value")
                .addStatement("$T copy = new $T(handlers)", ParameterizedTypeName.get(listClass, handlerClass), arrayListClass)
                .beginControlFlow("for ($T handler : copy)", handlerClass)
                .addStatement("handler.onPropertyChange(property, value)")
                .endControlFlow()
                .build());

        // Override Setters
        for (ExecutableElement method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(typeElement))) {
             if (method.getSimpleName().toString().startsWith("set") &&
                 method.getParameters().size() == 1 &&
                 !method.getModifiers().contains(Modifier.STATIC) &&
                 !method.getModifiers().contains(Modifier.FINAL) &&
                 !method.getModifiers().contains(Modifier.PRIVATE)) {

                 String propertyName = method.getSimpleName().toString().substring(3);
                 if (propertyName.length() > 0) {
                     propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);

                     MethodSpec.Builder override = MethodSpec.overriding(method);
                     override.addStatement("super.$L($L)", method.getSimpleName(), method.getParameters().get(0).getSimpleName());
                     override.addStatement("firePropertyChange($S, $L)", propertyName, method.getParameters().get(0).getSimpleName());
                     proxyBuilder.addMethod(override.build());
                 }
             }
        }

        JavaFile.builder(packageName, proxyBuilder.build())
                .build()
                .writeTo(processingEnv.getFiler());

        // Generate validator if any fields have constraint annotations
        List<VariableElement> constrainedFields = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            if (field.getAnnotation(NotNull.class) != null
                    || field.getAnnotation(NotEmpty.class) != null
                    || field.getAnnotation(Size.class) != null
                    || field.getAnnotation(Min.class) != null
                    || field.getAnnotation(Max.class) != null
                    || field.getAnnotation(Pattern.class) != null) {
                constrainedFields.add(field);
            }
        }
        if (!constrainedFields.isEmpty()) {
            generateValidator(typeElement, constrainedFields, packageName, typeName);
        }
    }

    private void generateValidator(TypeElement typeElement, List<VariableElement> constrainedFields,
            String packageName, ClassName typeName) throws IOException {
        String validatorName = typeElement.getSimpleName() + "_Validator";
        ClassName validationResultClass = ClassName.get("dev.verrai.api.validation", "ValidationResult");
        ClassName validatorInterface = ClassName.get("dev.verrai.api.validation", "Validator");
        ClassName listClass = ClassName.get(List.class);
        ClassName arrayListClass = ClassName.get(ArrayList.class);
        ClassName stringClass = ClassName.get(String.class);

        TypeSpec.Builder validatorBuilder = TypeSpec.classBuilder(validatorName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(validatorInterface, typeName));

        // static validateField(String fieldName, Object value)
        MethodSpec.Builder validateFieldMethod = MethodSpec.methodBuilder("validateField")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(validationResultClass)
                .addParameter(String.class, "fieldName")
                .addParameter(Object.class, "value");

        validateFieldMethod.addStatement("$T errors = new $T<>()",
                ParameterizedTypeName.get(listClass, stringClass), arrayListClass);
        validateFieldMethod.beginControlFlow("switch (fieldName)");

        for (VariableElement field : constrainedFields) {
            validateFieldMethod.addCode("case $S:\n", field.getSimpleName().toString());

            NotNull notNull = field.getAnnotation(NotNull.class);
            NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
            Size size = field.getAnnotation(Size.class);
            Min min = field.getAnnotation(Min.class);
            Max max = field.getAnnotation(Max.class);
            Pattern pattern = field.getAnnotation(Pattern.class);

            if (notNull != null) {
                validateFieldMethod.addCode("  if (value == null) errors.add($S);\n", notNull.message());
            }
            if (notEmpty != null) {
                validateFieldMethod.addCode(
                        "  if (value == null || value.toString().isEmpty()) errors.add($S);\n",
                        notEmpty.message());
            }
            if (size != null) {
                validateFieldMethod.addCode(
                        "  if (value != null && (value.toString().length() < $L || value.toString().length() > $L)) errors.add($S);\n",
                        size.min(), size.max(), size.message());
            }
            if (min != null) {
                validateFieldMethod.beginControlFlow("  if (value != null)");
                validateFieldMethod.beginControlFlow("  try");
                validateFieldMethod.addCode(
                        "  if (Long.parseLong(value.toString()) < $LL) errors.add($S);\n",
                        min.value(), min.message());
                validateFieldMethod.nextControlFlow("  catch ($T e)", NumberFormatException.class);
                validateFieldMethod.addStatement("  errors.add($S)", min.message());
                validateFieldMethod.endControlFlow();
                validateFieldMethod.endControlFlow();
            }
            if (max != null) {
                validateFieldMethod.beginControlFlow("  if (value != null)");
                validateFieldMethod.beginControlFlow("  try");
                validateFieldMethod.addCode(
                        "  if (Long.parseLong(value.toString()) > $LL) errors.add($S);\n",
                        max.value(), max.message());
                validateFieldMethod.nextControlFlow("  catch ($T e)", NumberFormatException.class);
                validateFieldMethod.addStatement("  errors.add($S)", max.message());
                validateFieldMethod.endControlFlow();
                validateFieldMethod.endControlFlow();
            }
            if (pattern != null) {
                validateFieldMethod.addCode(
                        "  if (value != null && !java.util.regex.Pattern.matches($S, value.toString())) errors.add($S);\n",
                        pattern.regexp(), pattern.message());
            }

            validateFieldMethod.addCode("  break;\n");
        }

        validateFieldMethod.addCode("default:\n  break;\n");
        validateFieldMethod.endControlFlow();
        validateFieldMethod.addStatement(
                "return errors.isEmpty() ? $T.valid() : $T.invalid(errors)",
                validationResultClass, validationResultClass);

        validatorBuilder.addMethod(validateFieldMethod.build());

        // validate(T model) — calls validateField per constrained field
        MethodSpec.Builder validateMethod = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(validationResultClass)
                .addParameter(typeName, "model");

        validateMethod.addStatement("$T errors = new $T<>()",
                ParameterizedTypeName.get(listClass, stringClass), arrayListClass);

        for (VariableElement field : constrainedFields) {
            String fieldName = field.getSimpleName().toString();
            String capProp = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String getterName = "get" + capProp;
            String resultVar = "result_" + fieldName;
            validateMethod.addStatement("$T $L = validateField($S, model.$L())",
                    validationResultClass, resultVar, fieldName, getterName);
            validateMethod.beginControlFlow("if (!$L.isValid())", resultVar);
            validateMethod.addStatement("errors.addAll($L.getMessages())", resultVar);
            validateMethod.endControlFlow();
        }

        validateMethod.addStatement(
                "return errors.isEmpty() ? $T.valid() : $T.invalid(errors)",
                validationResultClass, validationResultClass);

        validatorBuilder.addMethod(validateMethod.build());

        JavaFile.builder(packageName, validatorBuilder.build())
                .build()
                .writeTo(processingEnv.getFiler());
    }
}
