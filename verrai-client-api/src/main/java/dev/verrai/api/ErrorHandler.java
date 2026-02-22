package dev.verrai.api;

/**
 * Callback interface for centralized application error handling.
 * Register instances via {@link ErrorBus#register(ErrorHandler)}.
 */
public interface ErrorHandler {
    void onError(String context, Throwable t);
}
