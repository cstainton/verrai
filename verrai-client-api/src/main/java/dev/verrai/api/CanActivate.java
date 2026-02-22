package dev.verrai.api;

import java.util.Map;

/**
 * Implement on a {@code @Page} class to control whether navigation <em>to</em> this
 * page is permitted.
 *
 * <p>The generated router calls {@code canActivate} after instantiating the page but
 * before calling any {@code @PageShowing} methods. Returning {@code false} aborts the
 * navigation and re-navigates to the previous page (if one exists).
 *
 * @see CanDeactivate
 */
public interface CanActivate {
    /**
     * @param toRole the role being navigated to (this page's role)
     * @param state  URL state parameters for the navigation
     * @return {@code true} to allow navigation, {@code false} to block it
     */
    boolean canActivate(String toRole, Map<String, String> state);
}
