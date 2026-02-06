package uk.co.instanto.tearay.processor;

import uk.co.instanto.tearay.api.DataField;
import uk.co.instanto.tearay.api.Templated;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("uk.co.instanto.tearay.api.Templated")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class TemplatedProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Templated.class)) {
            if (element.getKind() != ElementKind.CLASS) continue;
            try {
                processType((TypeElement) element);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error processing @Templated: " + e.getMessage(), element);
                e.printStackTrace();
            }
        }
        return true;
    }

    private void processType(TypeElement typeElement) throws IOException {
        Templated annotation = typeElement.getAnnotation(Templated.class);
        String templateName = annotation.value();
        if (templateName.isEmpty()) {
            templateName = typeElement.getSimpleName() + ".html";
        }

        String htmlContent = readTemplate(typeElement, templateName);
        if (htmlContent == null) return;

        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String binderName = typeElement.getSimpleName() + "_Binder";

        ClassName htmlElementClass = ClassName.get("org.teavm.jso.dom.html", "HTMLElement");
        ClassName windowClass = ClassName.get("org.teavm.jso.browser", "Window");
        ClassName documentClass = ClassName.get("org.teavm.jso.dom.html", "HTMLDocument");
        ClassName nodeListClass = ClassName.get("org.teavm.jso.dom.xml", "NodeList");
        ClassName elementClass = ClassName.get("org.teavm.jso.dom.xml", "Element");

        MethodSpec.Builder bindMethod = MethodSpec.methodBuilder("bind")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC)
                .returns(htmlElementClass)
                .addParameter(com.squareup.javapoet.TypeName.get(typeElement.asType()), "target");

        bindMethod.addStatement("$T doc = $T.current().getDocument()", documentClass, windowClass);
        bindMethod.addStatement("$T root = doc.createElement($S)", htmlElementClass, "div");

        // Simple escaping for the demo.
        String escapedHtml = htmlContent.replace("\n", " ");
        bindMethod.addStatement("root.setInnerHTML($S)", escapedHtml);

        // Assign root if a field "element" exists (Convention for this PoC)
        for (VariableElement field : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
             if (field.getSimpleName().toString().equals("element") &&
                 com.squareup.javapoet.TypeName.get(field.asType()).equals(htmlElementClass)) {
                 bindMethod.addStatement("target.element = root");
             }
        }

        // Collect data fields
        List<VariableElement> dataFields = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            if (field.getAnnotation(DataField.class) != null) {
                dataFields.add(field);
            }
        }

        if (!dataFields.isEmpty()) {
            // Traverse DOM once
            // Declare variables
            for (VariableElement field : dataFields) {
                 bindMethod.addStatement("$T el_$L = null", htmlElementClass, field.getSimpleName());
            }

            // Use wildcard ? extends Element to handle NodeList<HTMLElement> returned by querySelectorAll on HTMLElement
            bindMethod.addStatement("$T<? extends $T> candidates = root.querySelectorAll($S)", nodeListClass, elementClass, "[data-field]");
            bindMethod.beginControlFlow("for (int i = 0; i < candidates.getLength(); i++)");
            bindMethod.addStatement("$T candidate = candidates.get(i)", elementClass);
            bindMethod.addStatement("$T key = candidate.getAttribute($S)", String.class, "data-field");
            bindMethod.beginControlFlow("switch (key)");

            for (VariableElement field : dataFields) {
                DataField dataField = field.getAnnotation(DataField.class);
                String dataFieldName = dataField.value();
                if (dataFieldName.isEmpty()) {
                    dataFieldName = field.getSimpleName().toString();
                }
                bindMethod.addCode("case $S:\n", dataFieldName);
                bindMethod.addStatement("  if (el_$L == null) el_$L = ($T) candidate", field.getSimpleName(), field.getSimpleName(), htmlElementClass);
                bindMethod.addStatement("  break");
            }
            bindMethod.endControlFlow(); // switch
            bindMethod.endControlFlow(); // for

            for (VariableElement field : dataFields) {
                // Reuse existing binding logic structure but check el_field != null
                bindMethod.beginControlFlow("if (el_$L != null)", field.getSimpleName());

                // Check if the field type is HTMLElement
                if (processingEnv.getTypeUtils().isAssignable(field.asType(), processingEnv.getElementUtils().getTypeElement("org.teavm.jso.dom.html.HTMLElement").asType())) {
                    bindMethod.addStatement("target.$L = ($T) el_$L",
                        field.getSimpleName(),
                        com.squareup.javapoet.TypeName.get(field.asType()),
                        field.getSimpleName());
                } else {
                    // Assume it is a nested component.
                    bindMethod.beginControlFlow("if (target.$L != null)", field.getSimpleName());

                    bindMethod.addStatement("$T widgetElement = target.$L.element", htmlElementClass, field.getSimpleName());
                    bindMethod.beginControlFlow("if (widgetElement != null)");

                    // Merge attributes
                    bindMethod.addStatement("String currentClasses = widgetElement.getClassName()");
                    bindMethod.addStatement("String placeholderClasses = el_$L.getClassName()", field.getSimpleName());
                    bindMethod.beginControlFlow("if (placeholderClasses != null && !placeholderClasses.isEmpty())");
                    bindMethod.addStatement("widgetElement.setClassName((currentClasses != null ? currentClasses + \" \" : \"\") + placeholderClasses)");
                    bindMethod.endControlFlow();

                    bindMethod.addStatement("String placeholderId = el_$L.getAttribute(\"id\")", field.getSimpleName());
                    bindMethod.beginControlFlow("if (placeholderId != null && !placeholderId.isEmpty())");
                    bindMethod.addStatement("widgetElement.setAttribute(\"id\", placeholderId)");
                    bindMethod.endControlFlow();

                    bindMethod.addStatement("String placeholderStyle = el_$L.getAttribute(\"style\")", field.getSimpleName());
                    bindMethod.beginControlFlow("if (placeholderStyle != null && !placeholderStyle.isEmpty())");
                    bindMethod.addStatement("String currentStyle = widgetElement.getAttribute(\"style\")");
                    bindMethod.addStatement("widgetElement.setAttribute(\"style\", (currentStyle != null ? currentStyle + \";\" : \"\") + placeholderStyle)");
                    bindMethod.endControlFlow();

                    bindMethod.addStatement("el_$L.getParentNode().replaceChild(widgetElement, el_$L)", field.getSimpleName(), field.getSimpleName());
                    bindMethod.endControlFlow();

                    bindMethod.endControlFlow();
                }

                bindMethod.endControlFlow();
            }
        }

        bindMethod.addStatement("return root");

        TypeSpec binderClass = TypeSpec.classBuilder(binderName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addMethod(bindMethod.build())
                .build();

        JavaFile.builder(packageName, binderClass)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private String readTemplate(TypeElement typeElement, String templateName) {
         String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
         try {
             FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, packageName, templateName);
             return resource.getCharContent(true).toString();
         } catch (Exception e) {
             try {
                 FileObject resource = processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, packageName, templateName);
                 return resource.getCharContent(true).toString();
             } catch (Exception ex) {
                 processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not find template: " + templateName + " in package " + packageName, typeElement);
                 return null;
             }
         }
    }
}
