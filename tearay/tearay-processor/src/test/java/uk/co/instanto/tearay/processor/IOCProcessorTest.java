package uk.co.instanto.tearay.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.CompilationSubject.assertThat;

public class IOCProcessorTest {

    @Test
    public void testSimpleBean() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
            "uk.co.instanto.tearay.processor.SimpleBean",
            "package uk.co.instanto.tearay.processor;",
            "",
            "import uk.co.instanto.tearay.api.ApplicationScoped;",
            "",
            "@ApplicationScoped",
            "public class SimpleBean {",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new IOCProcessor())
            .compile(source);

        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("uk.co.instanto.tearay.processor.SimpleBean_Factory");

        assertThat(compilation)
            .generatedSourceFile("uk.co.instanto.tearay.processor.SimpleBean_Factory")
            .contentsAsUtf8String()
            .contains("getInstance()");
    }
}
