package com.example.udyamsetubackend.auth.dto;

/**
 * Current sign-in state and the CSRF token for the current session.
 *
 * <p>The token is bound to the server-side session. The frontend keeps it in memory only, and sends it in the
 * header named by {@code csrfHeaderName} on every POST, PUT, PATCH and DELETE request. It is never stored in a
 * cookie, localStorage or sessionStorage, and never placed in a URL.
 *
 * @param authenticated  true when the caller has a valid owner session
 * @param username       the signed-in owner, or null when anonymous
 * @param csrfToken      the CSRF token for the current session (never null)
 * @param csrfHeaderName the request header that must carry {@code csrfToken}, currently {@code X-XSRF-TOKEN}
 */
public record SessionResponse(boolean authenticated, String username, String csrfToken, String csrfHeaderName) {

    public static SessionResponse anonymous(String csrfToken, String csrfHeaderName) {
        return new SessionResponse(false, null, csrfToken, csrfHeaderName);
    }

    public static SessionResponse signedIn(String username, String csrfToken, String csrfHeaderName) {
        return new SessionResponse(true, username, csrfToken, csrfHeaderName);
    }
}
