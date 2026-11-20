package com.example.udyamsetubackend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.function.Function;

/**
 * Forces every Spring Boot context started from the test classpath to use the dedicated
 * {@code udyamsetu_test} database and the restricted {@code udyamsetu_test_user} account.
 *
 * <p>Registered in {@code src/test/resources/META-INF/spring.factories}. The URL, username and password
 * are placed in a property source at the highest priority, so they override {@code application.yaml},
 * {@code config/local.yaml}, environment variables and system properties.
 *
 * <p>The test database credentials come only from the environment variables
 * {@value #USERNAME_VARIABLE} and {@value #PASSWORD_VARIABLE}. If either is missing or blank, startup
 * fails immediately. Nothing falls back to local, committed or root credentials. Error messages name the
 * variable but never print its value.
 */
public class TestDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String TEST_DATABASE_URL = "jdbc:mysql://localhost:3306/udyamsetu_test";
    public static final String TEST_DATABASE_NAME = "udyamsetu_test";
    public static final String TEST_DATABASE_USERNAME = "udyamsetu_test_user";

    /** Tests must never create or alter schema. The test database is prepared by an administrator. */
    public static final String TEST_DDL_AUTO = "validate";

    public static final String USERNAME_VARIABLE = "UDYAMSETU_TEST_DB_USERNAME";
    public static final String PASSWORD_VARIABLE = "UDYAMSETU_TEST_DB_PASSWORD";

    static final String PROPERTY_SOURCE_NAME = "udyamsetu-test-datasource";

    private final Function<String, String> variableLookup;

    /** Used by Spring Boot through {@code spring.factories}. Reads the real process environment. */
    public TestDatabaseEnvironmentPostProcessor() {
        this(System::getenv);
    }

    /** Test seam: lets tests supply the variables without touching the real environment. */
    TestDatabaseEnvironmentPostProcessor(Function<String, String> variableLookup) {
        this.variableLookup = variableLookup;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String username = requiredVariable(USERNAME_VARIABLE);
        String password = requiredVariable(PASSWORD_VARIABLE);

        if (!TEST_DATABASE_USERNAME.equals(username)) {
            throw new IllegalStateException(USERNAME_VARIABLE + " must be exactly '" + TEST_DATABASE_USERNAME
                    + "'. Tests refuse to run with any other database account.");
        }

        environment.getPropertySources().addFirst(new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of(
                        "spring.datasource.url", TEST_DATABASE_URL,
                        "spring.datasource.username", username,
                        "spring.datasource.password", password,
                        "spring.jpa.hibernate.ddl-auto", TEST_DDL_AUTO)));
    }

    /** Run after ConfigData has loaded application.yaml and config/local.yaml, so this value is the final word. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String requiredVariable(String name) {
        String value = variableLookup.apply(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set or is blank. "
                    + "Tests need the dedicated test database account. Set " + USERNAME_VARIABLE
                    + " and " + PASSWORD_VARIABLE + ". No fallback credentials are used.");
        }
        return value;
    }
}
