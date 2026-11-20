package com.example.udyamsetubackend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Startup validation of the owner account. {@link ApplicationContextRunner} does not run environment
 * post-processors, so these tests never touch the test database guard.
 */
class OwnerAccountPropertiesTest {

    private static final String USERNAME = "app.security.owner.username=owner";
    private static final String VALID_HASH = "$2a$10$" + "a".repeat(53);
    private static final String HASH = "app.security.owner.password-hash=" + VALID_HASH;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(OwnerConfiguration.class);

    @Test
    void validUsernameAndBcryptHashStartTheContext() {
        runner.withPropertyValues(USERNAME, HASH).run(context -> {
            assertThat(context).hasNotFailed();
            assertEquals("owner", context.getBean(OwnerAccountProperties.class).username());
        });
    }

    @Test
    void missingUsernameFailsStartup() {
        runner.withPropertyValues(HASH).run(context -> {
            assertThat(context).hasFailed();
            assertThat(messages(context.getStartupFailure())).contains("app.security.owner.username");
        });
    }

    @Test
    void blankUsernameFailsStartup() {
        runner.withPropertyValues("app.security.owner.username=   ", HASH).run(context -> {
            assertThat(context).hasFailed();
            assertThat(messages(context.getStartupFailure())).contains("app.security.owner.username");
        });
    }

    @Test
    void usernameWithSurroundingSpacesIsRejected() {
        assertThrows(IllegalStateException.class, () -> new OwnerAccountProperties(" owner", VALID_HASH));
        assertThrows(IllegalStateException.class, () -> new OwnerAccountProperties("owner ", VALID_HASH));
    }

    @Test
    void usernameOverOneHundredCharactersIsRejected() {
        String tooLong = "u".repeat(101);
        assertThrows(IllegalStateException.class, () -> new OwnerAccountProperties(tooLong, VALID_HASH));
    }

    @Test
    void missingHashFailsStartup() {
        runner.withPropertyValues(USERNAME).run(context -> {
            assertThat(context).hasFailed();
            assertThat(messages(context.getStartupFailure())).contains("app.security.owner.password-hash");
        });
    }

    @Test
    void noOwnerPropertiesAtAllFailsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void plainPasswordInTheHashFieldIsRejectedWithoutBeingEchoed() {
        String plainPassword = "plain-password-must-not-appear";
        runner.withPropertyValues(USERNAME, "app.security.owner.password-hash=" + plainPassword).run(context -> {
            assertThat(context).hasFailed();
            String messages = messages(context.getStartupFailure());
            assertThat(messages).contains("app.security.owner.password-hash");
            assertFalse(messages.contains(plainPassword), "the rejected value must never appear in the failure");
        });
    }

    @Test
    void hashWithWrongLengthIsRejected() {
        runner.withPropertyValues(USERNAME, "app.security.owner.password-hash=$2a$10$short").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void hashWithUnsupportedAlgorithmPrefixIsRejected() {
        String md5Style = "$1$" + "a".repeat(56);
        runner.withPropertyValues(USERNAME, "app.security.owner.password-hash=" + md5Style).run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void toStringMasksTheHash() {
        OwnerAccountProperties properties = new OwnerAccountProperties("owner", VALID_HASH);

        assertFalse(properties.toString().contains(VALID_HASH));
        assertThat(properties.toString()).contains("owner").contains("********");
    }

    /** Collects every message in the cause chain, so a wrapped startup failure can still be inspected. */
    private static String messages(Throwable failure) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                text.append(current.getMessage()).append('\n');
            }
        }
        return text.toString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OwnerAccountProperties.class)
    static class OwnerConfiguration {
    }
}
