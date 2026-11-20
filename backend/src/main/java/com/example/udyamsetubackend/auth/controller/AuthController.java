package com.example.udyamsetubackend.auth.controller;

import com.example.udyamsetubackend.auth.dto.LoginRequest;
import com.example.udyamsetubackend.auth.dto.SessionResponse;
import com.example.udyamsetubackend.auth.service.AuthService;
import com.example.udyamsetubackend.auth.service.LoginThrottledException;
import com.example.udyamsetubackend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Owner sign-in, session check and sign-out.
 *
 * <p>The CSRF token is stored in the HTTP session ({@link CsrfTokenRepository} is session-backed) and returned in
 * the JSON body of {@code GET /api/auth/session} and {@code POST /api/auth/login}. It is not written to any cookie.
 * <ul>
 *   <li>Session check: returns the current token, creating an anonymous session when there is none.</li>
 *   <li>Login: changes the session ID and replaces the CSRF token, so the pre-login token stops working.</li>
 *   <li>Logout: invalidates the session, which removes its CSRF state along with it.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";

    private final AuthService authService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SecurityContextRepository securityContextRepository;
    private final boolean secureCookies;

    public AuthController(AuthService authService,
                          CsrfTokenRepository csrfTokenRepository,
                          SecurityContextRepository securityContextRepository,
                          @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) {
        this.authService = authService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.securityContextRepository = securityContextRepository;
        this.secureCookies = secureCookies;
    }

    /**
     * Public. Reports the sign-in state and returns the session's CSRF token. Calling this on a page load or
     * reload gives the frontend a valid token whether or not the owner is signed in.
     */
    @GetMapping("/session")
    public SessionResponse session(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrf = csrfTokenRepository.loadDeferredToken(request, response).get();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isSignedIn(authentication)) {
            return SessionResponse.signedIn(authentication.getName(), csrf.getToken(), csrf.getHeaderName());
        }
        return SessionResponse.anonymous(csrf.getToken(), csrf.getHeaderName());
    }

    /**
     * Public, but requires a valid CSRF token from the pre-login session. Starts an owner session and returns a
     * new CSRF token for it.
     */
    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        Authentication authentication = authService.login(request.username(), request.password());
        CsrfToken fresh = establishOwnerSession(authentication, httpRequest, httpResponse);
        return SessionResponse.signedIn(authentication.getName(), fresh.getToken(), fresh.getHeaderName());
    }

    /**
     * Requires an owner session and a valid CSRF token (both enforced by the security chain). Invalidates the
     * session, which removes its CSRF token, and expires the session cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        expireSessionCookie(request, response);
        return ResponseEntity.noContent().build();
    }

    private CsrfToken establishOwnerSession(Authentication authentication,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        // Session fixation protection: the session ID changes at the moment of authentication.
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // Login rotation: replacing the stored token makes the pre-login token invalid for this session.
        CsrfToken fresh = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(fresh, request, response);
        return fresh;
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }

    private void expireSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        String name = Optional.ofNullable(request.getServletContext().getSessionCookieConfig().getName())
                .orElse(DEFAULT_SESSION_COOKIE_NAME);
        ResponseCookie expired = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    /** One generic message for every credential failure, so the response does not reveal which part was wrong. */
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(LoginThrottledException.class)
    ResponseEntity<ErrorResponse> handleThrottled(LoginThrottledException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }
}
