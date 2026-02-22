package dev.verrai.rpc.processor;

import dev.verrai.rpc.common.serialization.CodecLoader;
import dev.verrai.rpc.common.serialization.JsonCodecRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CodecRegistryGeneratorTest {

    private Filer filer;
    private CodecRegistryGenerator generator;

    @Before
    public void setUp() {
        filer = mock(Filer.class);
        generator = new CodecRegistryGenerator(filer) {
            @Override
            protected com.squareup.javapoet.ClassName getClassName(TypeElement element) {
                String qName = element.getQualifiedName().toString();
                String simpleName = element.getSimpleName().toString();
                int lastDot = qName.lastIndexOf('.');
                String packageName = lastDot > 0 ? qName.substring(0, lastDot) : "";
                return com.squareup.javapoet.ClassName.get(packageName, simpleName);
            }
        };
    }

    @Test
    public void testGenerateEmpty() throws IOException {
        String result = generator.generate(Collections.emptySet());
        assertNull(result);
        verify(filer, never()).createSourceFile(anyString(), any());
    }

    @Test
    public void testGenerateSingle() throws IOException {
        // Setup mock TypeElement
        TypeElement element = mockTypeElement("com.example.MyType");

        // Setup Filer to capture output
        StringWriter outputWriter = new StringWriter();
        setupFiler(outputWriter);

        Set<TypeElement> elements = Collections.singleton(element);
        String generatedClassName = generator.generate(elements);

        assertNotNull(generatedClassName);
        assertTrue(generatedClassName.startsWith("dev.verrai.rpc.generated.CodecLoader_"));

        String output = outputWriter.toString();
        // Check content
        assertTrue(output.contains("package dev.verrai.rpc.generated;"));
        assertTrue(output.contains("import dev.verrai.rpc.common.serialization.CodecLoader;"));
        assertTrue(output.contains("import dev.verrai.rpc.common.serialization.JsonCodecRegistry;"));
        assertTrue(output.contains("public class CodecLoader_"));
        assertTrue(output.contains("implements CodecLoader"));
        // JavaPoet should import the types
        assertTrue(output.contains("import com.example.MyType;"));
        assertTrue(output.contains("import com.example.MyTypeJsonCodec;"));
        assertTrue(output.contains("JsonCodecRegistry.register(MyType.class, new MyTypeJsonCodec())"));
    }

    @Test
    public void testGenerateMultiple() throws IOException {
        TypeElement element1 = mockTypeElement("com.example.TypeA");
        TypeElement element2 = mockTypeElement("com.example.TypeB");

        StringWriter outputWriter = new StringWriter();
        setupFiler(outputWriter);

        Set<TypeElement> elements = new HashSet<>(Arrays.asList(element1, element2));
        String generatedClassName = generator.generate(elements);

        assertNotNull(generatedClassName);

        String output = outputWriter.toString();
        // Check imports and usage
        assertTrue(output.contains("import com.example.TypeA;"));
        assertTrue(output.contains("import com.example.TypeAJsonCodec;"));
        assertTrue(output.contains("import com.example.TypeB;"));
        assertTrue(output.contains("import com.example.TypeBJsonCodec;"));
        assertTrue(output.contains("JsonCodecRegistry.register(TypeA.class, new TypeAJsonCodec())"));
        assertTrue(output.contains("JsonCodecRegistry.register(TypeB.class, new TypeBJsonCodec())"));
    }

    private TypeElement mockTypeElement(String qualifiedName) {
        TypeElement element = mock(TypeElement.class);

        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
        String simpleName = lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;

        // Mock simple name
        Name sName = mock(Name.class);
        when(sName.toString()).thenReturn(simpleName);
        when(element.getSimpleName()).thenReturn(sName);

        // Mock nesting
        when(element.getNestingKind()).thenReturn(javax.lang.model.element.NestingKind.TOP_LEVEL);

        // Mock package
        javax.lang.model.element.PackageElement pkg = mock(javax.lang.model.element.PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn(packageName);
        when(pkg.getQualifiedName()).thenReturn(pkgName);
        when(pkg.isUnnamed()).thenReturn(packageName.isEmpty());

        when(element.getEnclosingElement()).thenReturn(pkg);

        // Also mock getQualifiedName just in case JavaPoet uses it as fallback or verification
        Name qName = mock(Name.class);
        when(qName.toString()).thenReturn(qualifiedName);
        when(element.getQualifiedName()).thenReturn(qName);

        return element;
    }

    private void setupFiler(StringWriter writer) throws IOException {
        JavaFileObject fileObject = mock(JavaFileObject.class);
        when(fileObject.openWriter()).thenReturn(writer);
        when(filer.createSourceFile(anyString(), any())).thenReturn(fileObject);
    }
}
