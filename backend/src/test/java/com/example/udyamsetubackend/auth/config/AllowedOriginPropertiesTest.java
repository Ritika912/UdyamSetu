package com.example.udyamsetubackend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Validation of the single allowed origin. Database-free. */
class AllowedOriginPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(OriginConfiguration.class);

    @Test
    void developmentHttpOnLocalhostIsAccepted() {
        assertAccepted("http://localhost:5173");
        assertAccepted("http://127.0.0.1:5173");
    }

    @Test
    void productionHttpsOriginIsAccepted() {
        assertAccepted("https://app.example.com");
        assertAccepted("https://app.example.com:8443");
    }

    @Test
    void plainHttpOnANonLocalHostIsRejected() {
        assertRejected("http://app.example.com");
    }

    @Test
    void wildcardIsRejected() {
        assertRejected("https://*.example.com");
        assertRejected("*");
    }

    @Test
    void trailingSlashAndPathAreRejected() {
        assertRejected("https://app.example.com/");
        assertRejected("https://app.example.com/dashboard");
    }

    @Test
    void queryFragmentAndUserInfoAreRejected() {
        assertRejected("https://app.example.com?x=1");
        assertRejected("https://app.example.com#frag");
        assertRejected("https://user@app.example.com");
    }

    @Test
    void uppercaseOriginIsRejectedBecauseBrowsersSendLowercase() {
        assertRejected("https://App.Example.com");
    }

    @Test
    void missingValueFailsStartupWithoutADefault() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankValueFailsStartup() {
        runner.withPropertyValues("app.security.allowed-origin=   ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void invalidValueErrorDoesNotEchoTheValue() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AllowedOriginProperties("http://sensitive-host.example.com"));

        assertEquals(false, error.getMessage().contains("sensitive-host"));
    }

    private void assertAccepted(String origin) {
        assertEquals(origin, new AllowedOriginProperties(origin).allowedOrigin());
    }

    private void assertRejected(String origin) {
        assertThrows(IllegalStateException.class, () -> new AllowedOriginProperties(origin),
                "must be rejected: " + origin);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AllowedOriginProperties.class)
    static class OriginConfiguration {
    }
}
