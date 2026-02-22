package dev.verrai.client.service;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AsyncStreamResultImplTest {

    @Test
    public void testConcurrentModification() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicInteger count = new AtomicInteger(0);

        AsyncStreamResult.Subscription sub1 = result.subscribe(item -> {
            count.incrementAndGet();
            // This will cause ConcurrentModificationException in the current implementation
            result.subscribe(i -> {}, e -> {}, () -> {});
        }, e -> {}, () -> {});

        result.onNext("hello");
        assertEquals(1, count.get());
    }

    @Test
    public void testUnsubscribeDuringCallback() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicInteger count = new AtomicInteger(0);

        final AsyncStreamResult.Subscription[] subHolder = new AsyncStreamResult.Subscription[1];
        subHolder[0] = result.subscribe(item -> {
            count.incrementAndGet();
            subHolder[0].cancel();
        }, e -> {}, () -> {});

        result.onNext("hello");
        assertEquals(1, count.get());

        result.onNext("world");
        assertEquals(1, count.get());
    }

    @Test
    public void testErrorPropagation() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        result.subscribe(item -> {}, errorRef::set, () -> {});

        Exception ex = new Exception("test error");
        result.onError(ex);

        assertEquals(ex, errorRef.get());
    }

    @Test
    public void testErrorPropagationWithMultipleSubscribers() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicInteger count = new AtomicInteger(0);

        result.subscribe(item -> {}, e -> count.incrementAndGet(), () -> {});
        result.subscribe(item -> {}, e -> count.incrementAndGet(), () -> {});

        result.onError(new Exception("test error"));

        assertEquals(2, count.get());
    }

    @Test
    public void testErrorPropagationClearsSubscribers() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicInteger nextCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        result.subscribe(item -> nextCount.incrementAndGet(), e -> errorCount.incrementAndGet(), () -> {});

        result.onError(new Exception("test error"));
        assertEquals(1, errorCount.get());

        result.onNext("after error");
        assertEquals(0, nextCount.get());
    }

    @Test
    public void testErrorInErrorHandlerDoesNotStopOtherSubscribers() {
        AsyncStreamResultImpl<String> result = new AsyncStreamResultImpl<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        result.subscribe(item -> {}, e -> {
            throw new RuntimeException("Oops");
        }, () -> {});

        result.subscribe(item -> {}, e -> errorCount.incrementAndGet(), () -> {});

        result.onError(new Exception("test error"));

        assertEquals(1, errorCount.get());
    }
}
