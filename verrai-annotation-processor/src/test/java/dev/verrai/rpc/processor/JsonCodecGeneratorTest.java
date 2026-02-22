package dev.verrai.rpc.processor;

import com.squareup.javapoet.JavaFile;
import org.junit.Test;
import org.mockito.Mockito;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonCodecGeneratorTest {

    @Test
    public void testGenerateJsonCodec() {
        // Mock TypeElement
        TypeElement typeElement = mock(TypeElement.class);
        PackageElement packageElement = mock(PackageElement.class);
        Name packageName = mock(Name.class);
        Name className = mock(Name.class);

        when(packageName.toString()).thenReturn("test.pkg");
        when(className.toString()).thenReturn("TestClass");

        when(packageElement.getQualifiedName()).thenReturn(packageName);
        when(packageElement.getKind()).thenReturn(ElementKind.PACKAGE); // Ensure package kind is correct

        when(typeElement.getEnclosingElement()).thenReturn(packageElement);
        when(typeElement.getSimpleName()).thenReturn(className);
        when(typeElement.getKind()).thenReturn(ElementKind.CLASS);
        when(typeElement.getNestingKind()).thenReturn(NestingKind.TOP_LEVEL);
        when(typeElement.getModifiers()).thenReturn(Collections.singleton(Modifier.PUBLIC));
        when(typeElement.asType()).thenReturn(mock(DeclaredType.class)); // ClassName.get might use asType() in some versions

        // Fields
        List<Element> fields = new ArrayList<>();
        fields.add(createField("name", "java.lang.String"));
        fields.add(createField("age", "int"));
        fields.add(createField("active", "boolean"));

        // Map<String, String> field
        fields.add(createMapField("properties"));

        when(typeElement.getEnclosedElements()).thenReturn((List)fields);

        JsonCodecGenerator generator = new JsonCodecGenerator() {
            @Override
            protected com.squareup.javapoet.ClassName getClassName(TypeElement typeElement) {
                return com.squareup.javapoet.ClassName.get("test.pkg", "TestClass");
            }
        };
        JavaFile file = generator.generate(typeElement);
        String code = file.toString();

        // Verification
        assertTrue(code.contains("public class TestClassJsonCodec implements JsonCodec<TestClass>"));

        // toJson checks
        assertTrue(code.contains("sb.append(\"\\\"name\\\":\")"));
        assertTrue(code.contains("sb.append(\"\\\"\").append(object.getName()).append(\"\\\"\")"));
        assertTrue(code.contains("sb.append(\"\\\"age\\\":\")"));
        assertTrue(code.contains("sb.append(object.getAge())"));

        // Map serialization
        // JavaPoet might use imports, so we check for the loop structure without full qualification if possible, or check imports
        // But simpler to just check key parts
        assertTrue(code.contains("object.getProperties().entrySet()"));
        assertTrue(code.contains("sb.append(\"\\\"\").append(entry.getKey()).append(\"\\\":\")"));

        // fromJson checks
        assertTrue(code.contains("if (key.equals(\"name\"))"));
        assertTrue(code.contains("object.setName(value.replace(\"\\\"\", \"\"))"));
        assertTrue(code.contains("if (key.equals(\"age\"))"));
        assertTrue(code.contains("object.setAge(Integer.parseInt(value))"));

        // Map deserialization
        assertTrue(code.contains("new HashMap<>()")); // HashMap is imported
        assertTrue(code.contains("map.put(k, v)"));
    }

    private Element createField(String name, String typeStr) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Collections.emptySet()); // Not static

        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn(name);
        when(field.getSimpleName()).thenReturn(fieldName);

        TypeMirror typeMirror = mock(TypeMirror.class);
        when(typeMirror.toString()).thenReturn(typeStr);
        when(field.asType()).thenReturn(typeMirror);

        return field;
    }

    private Element createMapField(String name) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Collections.emptySet());

        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn(name);
        when(field.getSimpleName()).thenReturn(fieldName);

        // Mock TypeMirror for Map<String, String>
        TypeMirror typeMirror = mock(TypeMirror.class);
        // The generator checks: type.toString().contains("java.util.Map") && type.toString().replace(" ", "").contains("<java.lang.String,java.lang.String>")
        when(typeMirror.toString()).thenReturn("java.util.Map<java.lang.String, java.lang.String>");
        when(field.asType()).thenReturn(typeMirror);

        return field;
    }
}
