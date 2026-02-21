package dev.verrai.api;

/**
 * Observer for navigation lifecycle events fired by the generated router.
 *
 * Both methods have {@code default} (no-op) implementations so implementors
 * need only override the events they care about.
 */
public interface NavigationListener {

    /**
     * Called after the outgoing page's {@code @PageHiding}/{@code @PageHidden}
     * lifecycle has completed and the DOM has been cleared, but before the new
     * page is instantiated and mounted.
     *
     * @param toRole the navigation role of the page being navigated to
     */
    default void onNavigating(String toRole) {}

    /**
     * Called after the new page has been successfully mounted into the DOM.
     * Not fired if a {@code @PageShowing} method triggered an immediate
     * re-navigation (the identity guard prevents the aborted mount).
     *
     * @param role the navigation role of the page that was mounted
     */
    default void onNavigated(String role) {}
}
