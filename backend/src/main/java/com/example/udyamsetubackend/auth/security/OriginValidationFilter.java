package com.example.udyamsetubackend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Rejects state-changing requests (POST, PUT, PATCH, DELETE) that do not come from the allowed application
 * origin. It runs before the CSRF filter, so a rejected request never reaches token validation.
 *
 * <ul>
 *   <li>{@code Sec-Fetch-Site: cross-site} is always rejected.</li>
 *   <li>If an {@code Origin} header is present, it must equal the allowed origin exactly. This rejects sibling
 *       subdomains, other schemes or ports, and the literal {@code null} origin.</li>
 *   <li>If there is no {@code Origin} header, the request is accepted only when {@code Sec-Fetch-Site} is absent,
 *       {@code same-origin} or {@code none}. A request with neither header comes from a non-browser client, and is
 *       still protected by the CSRF token and authentication.</li>
 * </ul>
 *
 * <p>Safe methods are not checked. Rejections are written by the shared {@link AccessDeniedHandler}, and the
 * rejected origin is never echoed back.
 */
public final class OriginValidationFilter extends OncePerRequestFilter {

    static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final String allowedOrigin;
    private final AccessDeniedHandler deniedHandler;

    public OriginValidationFilter(String allowedOrigin, AccessDeniedHandler deniedHandler) {
        this.allowedOrigin = allowedOrigin;
        this.deniedHandler = deniedHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!UNSAFE_METHODS.contains(request.getMethod()) || isAllowed(request, allowedOrigin)) {
            chain.doFilter(request, response);
            return;
        }
        deniedHandler.handle(request, response, new AccessDeniedException("Request origin is not allowed."));
    }

    /** Decision logic, separated for direct testing. */
    static boolean isAllowed(HttpServletRequest request, String allowedOrigin) {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (fetchSite != null && fetchSite.trim().equalsIgnoreCase("cross-site")) {
            return false;
        }
        String origin = request.getHeader("Origin");
        if (origin != null) {
            return allowedOrigin.equals(origin);
        }
        return fetchSite == null || fetchSite.trim().equalsIgnoreCase("same-origin")
                || fetchSite.trim().equalsIgnoreCase("none");
    }
}
