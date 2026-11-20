package com.example.udyamsetubackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free checks for the test datasource guard. None of these tests starts a Spring context with a
 * DataSource or opens a connection. They verify, using an injected variable lookup, that:
 * <ul>
 *   <li>the post-processor is registered with Spring Boot;</li>
 *   <li>missing, blank or non-test usernames fail closed without revealing any value;</li>
 *   <li>valid credentials are placed at the highest priority, ahead of config files.</li>
 * </ul>
 */
class TestDatabaseIsolationEnvironmentTest {

    private static final String TEST_PASSWORD = "unit-test-only-password-value";
    private static final String SPRING_FACTORIES = "META-INF/spring.factories";

    @Test
    void postProcessorIsRegisteredInTestSpringFactories() throws IOException {
        String expected = TestDatabaseEnvironmentPostProcessor.class.getName();
        assertTrue(springFactoriesMention(expected), "TestDatabaseEnvironmentPostProcessor must be registered in " + SPRING_FACTORIES);
    }

    @Test
    void missingUsernameFailsClosedWithoutRevealingPassword() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE, TEST_PASSWORD));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(new StandardEnvironment(), null));

        assertTrue(error.getMessage().contains(TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE));
        assertFalse(error.getMessage().contains(TEST_PASSWORD));
    }

    @Test
    void missingPasswordFailsClosedWithoutRevealingUsername() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE, TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(new StandardEnvironment(), null));

        assertTrue(error.getMessage().contains(TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE));
    }

    @Test
    void blankPasswordIsTreatedAsMissing() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE, TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME,
                TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE, "   "));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(new StandardEnvironment(), null));

        assertTrue(error.getMessage().contains(TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE));
    }

    @Test
    void blankUsernameIsTreatedAsMissing() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE, "",
                TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE, TEST_PASSWORD));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(new StandardEnvironment(), null));

        assertTrue(error.getMessage().contains(TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE));
    }

    @Test
    void rootOrAnyOtherAccountIsRejected() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE, "root",
                TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE, TEST_PASSWORD));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(new StandardEnvironment(), null));

        assertTrue(error.getMessage().contains(TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME));
        assertFalse(error.getMessage().contains(TEST_PASSWORD));
    }

    @Test
    void validCredentialsOverrideConfigFilesAtHighestPriority() {
        TestDatabaseEnvironmentPostProcessor processor = processorWith(Map.of(
                TestDatabaseEnvironmentPostProcessor.USERNAME_VARIABLE, TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME,
                TestDatabaseEnvironmentPostProcessor.PASSWORD_VARIABLE, TEST_PASSWORD));

        // Simulate config/local.yaml (which contains root) already loaded into the environment.
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> localConfig = new HashMap<>();
        localConfig.put("spring.datasource.url", "jdbc:mysql://localhost:3306/udyamsetu_db");
        localConfig.put("spring.datasource.username", "root");
        localConfig.put("spring.datasource.password", "local-only-password");
        environment.getPropertySources().addLast(new MapPropertySource("application-local", localConfig));

        processor.postProcessEnvironment(environment, null);

        assertEquals(TestDatabaseEnvironmentPostProcessor.PROPERTY_SOURCE_NAME,
                environment.getPropertySources().iterator().next().getName(), "Test source must be first");
        assertEquals(TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_URL, environment.getProperty("spring.datasource.url"));
        assertEquals(TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME, environment.getProperty("spring.datasource.username"));
        assertEquals(TEST_PASSWORD, environment.getProperty("spring.datasource.password"));
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    private static TestDatabaseEnvironmentPostProcessor processorWith(Map<String, String> variables) {
        return new TestDatabaseEnvironmentPostProcessor(variables::get);
    }

    private static boolean springFactoriesMention(String className) throws IOException {
        ClassLoader loader = TestDatabaseIsolationEnvironmentTest.class.getClassLoader();
        Enumeration<URL> resources = loader.getResources(SPRING_FACTORIES);
        for (URL resource : Collections.list(resources)) {
            try (InputStream in = resource.openStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (content.contains(className)) {
                    return true;
                }
            }
        }
        return false;
    }
}
