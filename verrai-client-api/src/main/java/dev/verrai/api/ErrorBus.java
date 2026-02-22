package dev.verrai.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Static error bus for centralized error dispatch across the application.
 *
 * <p>TeaVM compiles to single-threaded JavaScript so no synchronization is needed.
 * Register a custom handler early in the app lifecycle (e.g. before
 * {@code navigation.start()}) to replace the default {@code Window.alert} behaviour.
 *
 * <pre>{@code
 * ErrorBus.register((ctx, t) -> myErrorWidget.show(ctx + ": " + t.getMessage()));
 * navigation.start();
 * }</pre>
 */
public final class ErrorBus {

    private static final List<ErrorHandler> handlers = new ArrayList<>();

    private ErrorBus() {}

    /** Register an error handler. */
    public static void register(ErrorHandler handler) {
        handlers.add(handler);
    }

    /** Unregister a previously registered error handler. */
    public static void unregister(ErrorHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Dispatch an error to all registered handlers.
     * If no handlers are registered this is a no-op.
     */
    public static void dispatch(String context, Throwable t) {
        List<ErrorHandler> copy = new ArrayList<>(handlers);
        for (ErrorHandler h : copy) {
            h.onError(context, t);
        }
    }

    /** Returns {@code true} if at least one handler is registered. */
    public static boolean hasHandlers() {
        return !handlers.isEmpty();
    }
}
