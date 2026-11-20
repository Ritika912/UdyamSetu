package com.example.udyamsetubackend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

/**
 * Supplies a test-only owner account to every Spring context started from the test classpath, so the
 * application can start without real owner credentials.
 *
 * <p>The password is a fixed test value and is not a secret. The hash is generated at startup, so no
 * hash is committed. This class does not touch the database settings. Those remain in
 * {@link TestDatabaseEnvironmentPostProcessor}.
 */
public class TestOwnerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String OWNER_USERNAME = "owner-test";
    public static final String OWNER_PASSWORD = "owner-test-only-password";

    static final String PROPERTY_SOURCE_NAME = "udyamsetu-test-owner";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String hash = new BCryptPasswordEncoder().encode(OWNER_PASSWORD);
        environment.getPropertySources().addFirst(new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of(
                        "app.security.owner.username", OWNER_USERNAME,
                        "app.security.owner.password-hash", hash)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
