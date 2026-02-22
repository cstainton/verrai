package dev.verrai.rpc.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProtoGeneratorTest {

    @Mock
    private TypeElement typeElement;

    private ProtoGenerator protoGenerator = new ProtoGenerator();

    @Test
    public void testSimpleFields() {
        when(typeElement.getSimpleName()).thenReturn(mockName("SimpleMessage"));

        List<Element> enclosedElements = new ArrayList<>();
        enclosedElements.add(mockField("fieldString", "java.lang.String"));
        enclosedElements.add(mockField("fieldInt", "int"));
        enclosedElements.add(mockField("fieldLong", "long"));
        enclosedElements.add(mockField("fieldBoolean", "boolean"));

        // Add a static field which should be ignored
        Element staticField = mockField("staticField", "java.lang.String");
        Set<Modifier> modifiers = new HashSet<>();
        modifiers.add(Modifier.STATIC);
        when(staticField.getModifiers()).thenReturn(modifiers);
        enclosedElements.add(staticField);

        doReturn(enclosedElements).when(typeElement).getEnclosedElements();

        String result = protoGenerator.generate(typeElement, "com.example.proto");

        assertTrue("Missing syntax", result.contains("syntax = \"proto3\";"));
        assertTrue("Missing package", result.contains("package com.example.proto;"));
        assertTrue("Missing java_package", result.contains("option java_package = \"com.example.proto\";"));
        assertTrue("Missing message definition", result.contains("message SimpleMessage {"));
        assertTrue("Missing fieldString", result.contains("  string fieldString = 1;"));
        assertTrue(result.contains("  int32 fieldInt = 2;"));
        assertTrue(result.contains("  int64 fieldLong = 3;"));
        assertTrue(result.contains("  bool fieldBoolean = 4;"));
        // Static field should not be present
        assertFalse(result.contains("staticField"));
        assertTrue(result.endsWith("}\n"));
    }

    @Test
    public void testCollections() {
        when(typeElement.getSimpleName()).thenReturn(mockName("CollectionMessage"));

        List<Element> enclosedElements = new ArrayList<>();
        enclosedElements.add(mockField("stringList", "java.util.List<java.lang.String>"));
        enclosedElements.add(mockField("intList", "java.util.List<java.lang.Integer>"));
        enclosedElements.add(mockField("objList", "java.util.List<com.example.OtherObject>"));

        doReturn(enclosedElements).when(typeElement).getEnclosedElements();

        String result = protoGenerator.generate(typeElement, "com.example.proto");

        assertTrue(result.contains("repeated string stringList = 1;"));
        assertTrue(result.contains("repeated int32 intList = 2;"));
        assertTrue(result.contains("repeated OtherObject objList = 3;"));
    }

    @Test
    public void testMaps() {
        when(typeElement.getSimpleName()).thenReturn(mockName("MapMessage"));

        List<Element> enclosedElements = new ArrayList<>();
        enclosedElements.add(mockField("stringMap", "java.util.Map<java.lang.String,java.lang.String>"));

        doReturn(enclosedElements).when(typeElement).getEnclosedElements();

        String result = protoGenerator.generate(typeElement, "com.example.proto");

        // The generator currently assumes <string, string> for maps
        assertTrue(result.contains("map<string, string> stringMap = 1;"));
    }

    @Test
    public void testInnerEnums() {
        when(typeElement.getSimpleName()).thenReturn(mockName("EnumMessage"));
        Name qualifiedName = mockName("com.example.EnumMessage");
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);

        TypeElement enumElement = mock(TypeElement.class);
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);
        Name statusName = mockName("Status");
        when(enumElement.getSimpleName()).thenReturn(statusName);

        List<Element> enumConstants = new ArrayList<>();
        Element const1 = mock(Element.class);
        when(const1.getKind()).thenReturn(ElementKind.ENUM_CONSTANT);
        Name okName = mockName("OK");
        when(const1.getSimpleName()).thenReturn(okName);
        enumConstants.add(const1);

        Element const2 = mock(Element.class);
        when(const2.getKind()).thenReturn(ElementKind.ENUM_CONSTANT);
        Name errorName = mockName("ERROR");
        when(const2.getSimpleName()).thenReturn(errorName);
        enumConstants.add(const2);

        doReturn(enumConstants).when(enumElement).getEnclosedElements();

        List<Element> enclosedElements = new ArrayList<>();
        enclosedElements.add(enumElement);

        // Add a field using the enum
        enclosedElements.add(mockField("status", "Status"));

        doReturn(enclosedElements).when(typeElement).getEnclosedElements();

        String result = protoGenerator.generate(typeElement, "com.example.proto");

        assertTrue("Missing enum definition", result.contains("enum Status {"));
        assertTrue("Missing OK constant", result.contains("  OK = 0;"));
        assertTrue("Missing ERROR constant", result.contains("  ERROR = 1;"));
        assertTrue("Missing status field", result.contains("Status status = 1;"));
    }

    @Test
    public void testImports() {
        when(typeElement.getSimpleName()).thenReturn(mockName("ImportMessage"));
        Name qualifiedName = mockName("com.example.ImportMessage");
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);

        List<Element> enclosedElements = new ArrayList<>();
        enclosedElements.add(mockField("other", "com.example.other.OtherMessage"));
        enclosedElements.add(mockField("local", "com.example.ImportMessage.LocalInner")); // Should not import

        doReturn(enclosedElements).when(typeElement).getEnclosedElements();

        String result = protoGenerator.generate(typeElement, "com.example.proto");

        // Check imports
        assertTrue(result.contains("import \"com/example/other/proto/OtherMessage.proto\";"));
        assertFalse(result.contains("LocalInner.proto"));

        assertTrue(result.contains("OtherMessage other = 1;"));
    }

    private Element mockField(String name, String type) {
        Element field = mock(Element.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Collections.emptySet());

        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn(name);
        when(field.getSimpleName()).thenReturn(fieldName);

        TypeMirror typeMirror = mock(TypeMirror.class);
        when(typeMirror.toString()).thenReturn(type);
        when(field.asType()).thenReturn(typeMirror);

        return field;
    }

    private Name mockName(String name) {
        return new Name() {
            @Override
            public boolean contentEquals(CharSequence cs) {
                return name.contentEquals(cs);
            }

            @Override
            public int length() {
                return name.length();
            }

            @Override
            public char charAt(int index) {
                return name.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return name.subSequence(start, end);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
