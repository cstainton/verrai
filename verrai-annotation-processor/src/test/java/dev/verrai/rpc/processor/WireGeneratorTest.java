package dev.verrai.rpc.processor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WireGeneratorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Filer filer;

    @Mock
    private JavaFileObject javaFileObject;

    private WireGenerator wireGenerator;
    private Path tempDir;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(new StringWriter());

        tempDir = temporaryFolder.getRoot().toPath();
        wireGenerator = new WireGenerator(processingEnv, tempDir);
    }

    @Test
    public void testGenerateWireClasses() throws Exception {
        String protoContent = "syntax = \"proto3\";\n" +
                "package com.example;\n" +
                "message TestMessage {\n" +
                "  string text = 1;\n" +
                "}\n";

        wireGenerator.addProtoFile("simple.proto", protoContent);
        wireGenerator.generateWireClasses();

        verify(filer, atLeastOnce()).createSourceFile(eq("com.example.TestMessage"));
        verify(javaFileObject, atLeastOnce()).openWriter();
    }
}
