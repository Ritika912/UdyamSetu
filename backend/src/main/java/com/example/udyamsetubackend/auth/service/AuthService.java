package com.example.udyamsetubackend.auth.service;

import com.example.udyamsetubackend.auth.config.OwnerAccountProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Checks owner credentials and applies login throttling.
 *
 * <p>Every attempt is counted against the configured owner account, whatever username was submitted. Each
 * attempt first reserves a throttle slot, which atomically checks the lock. Only then is the password checked,
 * so a locked account is never tested, and concurrent requests cannot exceed the limit. Every reserved slot is
 * completed with a success or a failure, including on unexpected errors.
 */
@Service
public class AuthService {

    static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final OwnerAccountProperties owner;

    public AuthService(AuthenticationManager authenticationManager,
                       LoginAttemptService loginAttemptService,
                       OwnerAccountProperties owner) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
        this.owner = owner;
    }

    /**
     * @return the authenticated owner
     * @throws LoginThrottledException if the owner account is locked or too many attempts are in progress
     * @throws BadCredentialsException for any credential failure, with one generic message
     */
    public Authentication login(String username, String password) {
        String throttleKey = owner.username();
        if (!loginAttemptService.tryBeginAttempt(throttleKey)) {
            throw new LoginThrottledException();
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(throttleKey);
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        } catch (RuntimeException ex) {
            loginAttemptService.recordFailure(throttleKey);
            throw ex;
        }
        loginAttemptService.recordSuccess(throttleKey);
        return authentication;
    }
}
