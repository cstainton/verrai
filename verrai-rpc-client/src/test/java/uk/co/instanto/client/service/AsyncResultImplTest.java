package uk.co.instanto.client.service;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class AsyncResultImplTest {

    @Test
    public void testSuccess() {
        AsyncResultImpl<String> result = new AsyncResultImpl<>();
        AtomicReference<String> output = new AtomicReference<>();

        result.thenApply(s -> s + " world").thenAccept(output::set);

        result.complete("hello");

        assertEquals("hello world", output.get());
    }

    @Test
    public void testFailure() {
        AsyncResultImpl<String> result = new AsyncResultImpl<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicBoolean successCalled = new AtomicBoolean(false);

        result.thenAccept(s -> successCalled.set(true))
              .exceptionally(t -> {
                  errorRef.set(t);
                  return null;
              });

        Exception ex = new RuntimeException("fail");
        result.completeExceptionally(ex);

        assertFalse(successCalled.get());
        assertEquals(ex, errorRef.get());
    }

    @Test
    public void testChaining() {
        AsyncResultImpl<Integer> result = new AsyncResultImpl<>();
        AtomicReference<Integer> output = new AtomicReference<>();

        result.thenApply(i -> i * 2)
              .thenApply(i -> i + 1)
              .thenAccept(output::set);

        result.complete(5);

        assertEquals(Integer.valueOf(11), output.get());
    }

    @Test
    public void testExceptionInCallback() {
        AsyncResultImpl<String> result = new AsyncResultImpl<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        result.thenApply(s -> {
            throw new RuntimeException("error inside callback");
        }).exceptionally(t -> {
            errorRef.set(t);
            return null;
        });

        result.complete("trigger");

        assertNotNull(errorRef.get());
        assertEquals("error inside callback", errorRef.get().getMessage());
    }

    @Test
    public void testAlreadyCompleted() {
        AsyncResultImpl<String> result = new AsyncResultImpl<>();
        result.complete("done");

        AtomicReference<String> output = new AtomicReference<>();
        result.thenAccept(output::set);

        assertEquals("done", output.get());
    }

    @Test
    public void testMultipleCallbacks() {
        AsyncResultImpl<String> result = new AsyncResultImpl<>();
        AtomicReference<String> out1 = new AtomicReference<>();
        AtomicReference<String> out2 = new AtomicReference<>();

        result.thenAccept(out1::set);
        result.thenAccept(out2::set);

        result.complete("multi");

        assertEquals("multi", out1.get());
        assertEquals("multi", out2.get());
    }

    @Test
    public void testVoidResult() {
         AsyncResultImpl<String> result = new AsyncResultImpl<>();
         AtomicBoolean called = new AtomicBoolean(false);

         AsyncResult<Void> voidResult = result.thenAccept(s -> {});
         voidResult.thenAccept(v -> called.set(true));

         result.complete("test");

         assertTrue(called.get());
    }
}
