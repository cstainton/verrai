package uk.co.instanto.tearay.processor;

import uk.co.instanto.tearay.api.cdi.ApplicationScoped;
import uk.co.instanto.tearay.api.cdi.Dependent;
import uk.co.instanto.tearay.api.EntryPoint;
import uk.co.instanto.tearay.api.cdi.PostConstruct;
import uk.co.instanto.tearay.api.cdi.SessionScoped;
import uk.co.instanto.tearay.api.Templated;
import uk.co.instanto.tearay.api.Page;
import uk.co.instanto.tearay.api.cdi.Observes;
import uk.co.instanto.tearay.api.cdi.Event;
import uk.co.instanto.tearay.api.impl.EventBus;
import uk.co.instanto.tearay.api.impl.BeanLifecycleManager;
import uk.co.instanto.tearay.api.impl.SessionContext;
import com.squareup.javapoet.*;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "uk.co.instanto.tearay.api.cdi.ApplicationScoped",
    "uk.co.instanto.tearay.api.cdi.Dependent",
    "uk.co.instanto.tearay.api.cdi.SessionScoped",
    "uk.co.instanto.tearay.api.EntryPoint",
    "uk.co.instanto.tearay.api.Templated",
    "uk.co.instanto.tearay.api.Page"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class IOCProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Collect all beans
        Set<Element> beans = roundEnv.getElementsAnnotatedWith(ApplicationScoped.class).stream().collect(Collectors.toSet());
        beans.addAll(roundEnv.getElementsAnnotatedWith(Dependent.class));
        beans.addAll(roundEnv.getElementsAnnotatedWith(SessionScoped.class));
        beans.addAll(roundEnv.getElementsAnnotatedWith(EntryPoint.class));
        beans.addAll(roundEnv.getElementsAnnotatedWith(Templated.class));
        beans.addAll(roundEnv.getElementsAnnotatedWith(Page.class));

        for (Element element : beans) {
            if (element.getKind() != ElementKind.CLASS) continue;
            try {
                processBean((TypeElement) element);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error processing bean " + element + ": " + e.getMessage(), element);
                e.printStackTrace();
            }
        }

        // Handle Bootstrapper
        Set<? extends Element> entryPoints = roundEnv.getElementsAnnotatedWith(EntryPoint.class);
        if (!entryPoints.isEmpty()) {
            try {
                generateBootstrapper((TypeElement) entryPoints.iterator().next());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void processBean(TypeElement typeElement) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String factoryName = typeElement.getSimpleName() + "_Factory";
        ClassName typeName = ClassName.get(typeElement);

        boolean isDependent = typeElement.getAnnotation(Dependent.class) != null;
        boolean isSessionScoped = typeElement.getAnnotation(SessionScoped.class) != null;
        boolean isApplicationScoped = typeElement.getAnnotation(ApplicationScoped.class) != null;
        boolean isEntryPoint = typeElement.getAnnotation(EntryPoint.class) != null;
        boolean isPage = typeElement.getAnnotation(Page.class) != null;

        boolean isSingleton = (isApplicationScoped || isEntryPoint || isPage) && !isDependent && !isSessionScoped;
        boolean isTemplated = typeElement.getAnnotation(Templated.class) != null;

        TypeSpec.Builder factoryBuilder = TypeSpec.classBuilder(factoryName)
                .addModifiers(Modifier.PUBLIC);

        // Singleton Instance Holder
        if (isSingleton) {
            factoryBuilder.addField(FieldSpec.builder(typeName, "instance")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .build());
        }

        MethodSpec.Builder getMethod = MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(typeName);

        if (isSingleton) {
            getMethod.beginControlFlow("if (instance == null)")
                    .addStatement("instance = createInstance()")
                    .endControlFlow()
                    .addStatement("return instance");
        } else if (isSessionScoped) {
            ClassName sessionContextClass = ClassName.get(SessionContext.class);
            getMethod.addStatement("return $T.getInstance().get($T.class, () -> createInstance())", sessionContextClass, typeName);
        } else {
            getMethod.addStatement("return createInstance()");
        }

        factoryBuilder.addMethod(getMethod.build());

        // createInstance method
        MethodSpec.Builder createMethod = MethodSpec.methodBuilder("createInstance")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(typeName);

        createMethod.addStatement("$T bean = new $T()", typeName, typeName);

        ClassName eventBusClass = ClassName.get(EventBus.class);
        ClassName eventInterfaceClass = ClassName.get(Event.class);

        // Injection (collect fields from hierarchy)
        for (VariableElement field : collectFields(typeElement)) {
            if (field.getAnnotation(Inject.class) != null) {
                TypeMirror fieldType = processingEnv.getTypeUtils().asMemberOf((DeclaredType) typeElement.asType(), field);
                String fieldTypeStr = fieldType.toString();

                if (fieldTypeStr.equals("uk.co.instanto.tearay.api.Navigation")) {
                    ClassName dependencyFactory = ClassName.get("uk.co.instanto.tearay.impl", "NavigationImpl_Factory");
                    createMethod.addStatement("bean.$L = $T.getInstance()", field.getSimpleName(), dependencyFactory);
                } else if (fieldTypeStr.startsWith("uk.co.instanto.tearay.widgets.")) {
                    createMethod.addStatement("bean.$L = new $T()", field.getSimpleName(), ClassName.bestGuess(fieldTypeStr));
                } else if (fieldTypeStr.startsWith(Event.class.getName())) {
                    // Inject Event<T>
                    TypeMirror genericType = null;
                    if (fieldType instanceof DeclaredType) {
                        List<? extends TypeMirror> args = ((DeclaredType) fieldType).getTypeArguments();
                        if (!args.isEmpty()) genericType = args.get(0);
                    }

                    TypeSpec eventImpl = TypeSpec.anonymousClassBuilder("")
                            .addSuperinterface(ParameterizedTypeName.get(eventInterfaceClass,
                                (genericType != null ? TypeName.get(genericType) : TypeName.OBJECT)))
                            .addMethod(MethodSpec.methodBuilder("fire")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .addParameter(genericType != null ? TypeName.get(genericType) : TypeName.OBJECT, "event")
                                    .addStatement("$T.getInstance().fire(event)", eventBusClass)
                                    .build())
                            .build();

                    createMethod.addStatement("bean.$L = $L", field.getSimpleName(), eventImpl);

                } else if (fieldTypeStr.startsWith(Provider.class.getName())) {
                    // Inject Provider<T>
                    TypeMirror genericType = null;
                    if (fieldType instanceof DeclaredType) {
                        List<? extends TypeMirror> args = ((DeclaredType) fieldType).getTypeArguments();
                        if (!args.isEmpty()) genericType = args.get(0);
                    }

                    if (genericType != null) {
                        String genericTypeStr = genericType.toString();
                        ClassName dependencyFactory;
                        if (genericTypeStr.startsWith("uk.co.instanto.tearay.widgets.")) {
                             // Provider for Widgets (direct new)
                             ClassName widgetClass = ClassName.bestGuess(genericTypeStr);
                             createMethod.addStatement("bean.$L = () -> new $T()", field.getSimpleName(), widgetClass);
                        } else {
                             // Provider for Managed Bean
                             dependencyFactory = ClassName.bestGuess(genericTypeStr + "_Factory");
                             createMethod.addStatement("bean.$L = () -> $T.getInstance()", field.getSimpleName(), dependencyFactory);
                        }
                    }

                } else {
                    ClassName dependencyFactory = ClassName.bestGuess(fieldTypeStr + "_Factory");
                    createMethod.addStatement("bean.$L = $T.getInstance()", field.getSimpleName(), dependencyFactory);
                }
            }
        }

        // Templated Binding
        if (isTemplated) {
            ClassName binderClass = ClassName.get(packageName, typeElement.getSimpleName() + "_Binder");
            createMethod.addStatement("$T.bind(bean)", binderClass);
        }

        // Observer Registration
        for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
             for (VariableElement param : method.getParameters()) {
                 if (param.getAnnotation(Observes.class) != null) {
                     TypeMirror eventType = param.asType();
                     createMethod.addStatement("$T subscription = $T.getInstance().subscribe($T.class, (e) -> bean.$L(e))",
                         Runnable.class,
                         eventBusClass,
                         TypeName.get(eventType),
                         method.getSimpleName());

                     // Register with BeanLifecycleManager
                     createMethod.addStatement("$T.getInstance().register(bean, subscription)", ClassName.get(BeanLifecycleManager.class));
                 }
             }
        }

        // PostConstruct
        for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            if (method.getAnnotation(PostConstruct.class) != null) {
                createMethod.addStatement("bean.$L()", method.getSimpleName());
            }
        }

        createMethod.addStatement("return bean");
        factoryBuilder.addMethod(createMethod.build());

        JavaFile.builder(packageName, factoryBuilder.build())
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private List<VariableElement> collectFields(TypeElement typeElement) {
        List<VariableElement> fields = new java.util.ArrayList<>(ElementFilter.fieldsIn(typeElement.getEnclosedElements()));
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
            TypeElement superType = (TypeElement) ((DeclaredType) superclass).asElement();
            if (!superType.getQualifiedName().toString().equals("java.lang.Object")) {
                fields.addAll(collectFields(superType));
            }
        }
        return fields;
    }

    private void generateBootstrapper(TypeElement entryPoint) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(entryPoint).getQualifiedName().toString();
        ClassName entryPointFactory = ClassName.get(packageName, entryPoint.getSimpleName() + "_Factory");

        TypeSpec bootstrapper = TypeSpec.classBuilder("BootstrapperImpl")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("run")
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("$T instance = $T.getInstance()", ClassName.get(entryPoint), entryPointFactory)
                        .addStatement("instance.onModuleLoad()")
                        .build())
                .build();

        JavaFile.builder(packageName, bootstrapper)
                .build()
                .writeTo(processingEnv.getFiler());
    }
}
