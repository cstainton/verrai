package dev.verrai.api;

/**
 * Implement on a {@code @Page} class to control whether navigation <em>away from</em>
 * this page is permitted.
 *
 * <p>The generated router calls {@code canDeactivate} on the current page before any
 * outgoing lifecycle methods ({@code @PageHiding} etc.). Returning {@code false} aborts
 * the navigation and the current page remains mounted.
 *
 * @see CanActivate
 */
public interface CanDeactivate {
    /**
     * @return {@code true} to allow leaving this page, {@code false} to block navigation
     */
    boolean canDeactivate();
}
