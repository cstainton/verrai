package dev.verrai.rpc.processor;

import com.squareup.javapoet.*;
import dev.verrai.rpc.common.serialization.CodecLoader;
import dev.verrai.rpc.common.serialization.JsonCodecRegistry;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;

public class CodecRegistryGenerator {

    private final Filer filer;

    public CodecRegistryGenerator(Filer filer) {
        this.filer = filer;
    }

    /**
     * Generates a CodecLoader implementation for the given elements.
     * @return The fully qualified name of the generated class.
     */
    public String generate(Set<TypeElement> elements) throws IOException {
        if (elements.isEmpty()) return null;

        TypeElement first = elements.iterator().next();
        String elementPkg = ((javax.lang.model.element.PackageElement) first.getEnclosingElement()).getQualifiedName().toString();
        String pkg = elementPkg + ".generated";
        // Use a suffix based on the hash of the element names to be consistent
        int hash = elements.stream().map(e -> e.getQualifiedName().toString()).sorted().reduce("", String::concat).hashCode();
        String suffix = first.getSimpleName().toString() + "_" + Math.abs(hash);
        String className = "CodecLoader_" + suffix;

        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(CodecLoader.class);

        MethodSpec.Builder loadMethod = MethodSpec.methodBuilder("load")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(void.class);

        for (TypeElement element : elements) {
            ClassName typeName = ClassName.get(element);
            String currentPkg = ((javax.lang.model.element.PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
            ClassName codecName = ClassName.get(currentPkg, element.getSimpleName() + "JsonCodec");

            loadMethod.addStatement("$T.register($T.class, new $T())",
                    JsonCodecRegistry.class, typeName, codecName);
        }

        typeSpec.addMethod(loadMethod.build());

        JavaFile javaFile = JavaFile.builder(pkg, typeSpec.build()).build();
        javaFile.writeTo(filer);

        return pkg + "." + className;
    }
}
