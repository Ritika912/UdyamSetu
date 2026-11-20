package com.example.udyamsetubackend.auth.service;

import com.example.udyamsetubackend.auth.config.OwnerAccountProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Database-free unit tests for owner sign-in and its interaction with throttling. */
class AuthServiceTest {

    private static final String OWNER = "owner";
    private static final String HASH = "$2a$10$" + "a".repeat(53);

    private AuthenticationManager authenticationManager;
    private LoginAttemptService loginAttemptService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
        loginAttemptService = new LoginAttemptService(clock);
        authService = new AuthService(authenticationManager, loginAttemptService,
                new OwnerAccountProperties(OWNER, HASH));
    }

    @Test
    void validCredentialsReturnTheAuthentication() {
        Authentication authenticated = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);

        assertEquals(authenticated, authService.login(OWNER, "correct-password"));
    }

    @Test
    void wrongPasswordFailsWithOneGenericMessage() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("internal detail"));

        BadCredentialsException error = assertThrows(BadCredentialsException.class,
                () -> authService.login(OWNER, "wrong"));

        assertEquals(AuthService.INVALID_CREDENTIALS_MESSAGE, error.getMessage());
    }

    @Test
    void unknownUsernameGetsTheSameGenericMessage() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("user not found"));

        BadCredentialsException error = assertThrows(BadCredentialsException.class,
                () -> authService.login("not-the-owner", "anything"));

        assertEquals(AuthService.INVALID_CREDENTIALS_MESSAGE, error.getMessage());
    }

    @Test
    void fifthFailedAttemptLocksAndTheSixthIsRefusedWithoutCheckingThePassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(OWNER, "wrong"));
        }

        assertThrows(LoginThrottledException.class, () -> authService.login(OWNER, "correct-password"));
        verify(authenticationManager, times(5)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void successfulLoginResetsTheCounter() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenReturn(mock(Authentication.class))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(OWNER, "wrong"));
        }
        authService.login(OWNER, "correct-password");
        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(OWNER, "wrong"));
        }

        assertFalse(loginAttemptService.isLocked(OWNER), "the success cleared the counter");
    }

    @Test
    void concurrentLoginAttemptsReachTheAuthenticationManagerAtMostTheConfiguredLimit() throws Exception {
        AtomicInteger passwordChecks = new AtomicInteger();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenAnswer(invocation -> {
                    passwordChecks.incrementAndGet();
                    Thread.sleep(20); // widen the race window so attempts genuinely overlap
                    throw new BadCredentialsException("bad");
                });

        int threads = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger throttled = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        authService.login(OWNER, "wrong");
                    } catch (LoginThrottledException refused) {
                        throttled.incrementAndGet();
                    } catch (BadCredentialsException failed) {
                        // counted as a checked password
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(LoginAttemptService.MAX_FAILED_ATTEMPTS, passwordChecks.get(),
                "only the configured number of passwords may be checked");
        assertEquals(threads - LoginAttemptService.MAX_FAILED_ATTEMPTS, throttled.get(),
                "every other concurrent attempt must be refused without a password check");
    }

    @Test
    void lockedAccountNeverReachesTheAuthenticationManager() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(OWNER, "wrong"));
        }

        assertThrows(LoginThrottledException.class, () -> authService.login("other-name", "wrong"));

        verify(authenticationManager, times(5)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authenticationManager, never()).authenticate(
                org.mockito.ArgumentMatchers.argThat(token -> "other-name".equals(token.getName())));
        assertTrue(loginAttemptService.isLocked(OWNER));
    }
}
