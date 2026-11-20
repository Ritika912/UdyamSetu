package com.example.udyamsetubackend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the production profile and the shared application.yaml without starting a Spring context,
 * a database or the test database guard. Placeholders are resolved against only the YAML files and the
 * values each test supplies, so the real process environment never leaks into the result.
 */
class ProductionConfigurationTest {

    private static final String DB_URL_KEY = "spring.datasource.url";
    private static final String DB_USERNAME_KEY = "spring.datasource.username";
    private static final String DB_PASSWORD_KEY = "spring.datasource.password";
    private static final String OWNER_USERNAME_KEY = "app.security.owner.username";
    private static final String OWNER_HASH_KEY = "app.security.owner.password-hash";
    private static final String ALLOWED_ORIGIN_KEY = "app.security.allowed-origin";
    private static final String LOCAL_IMPORT_KEY = "spring.config.import";
    private static final String ON_PROFILE_KEY = "spring.config.activate.on-profile";

    private static final Map<String, String> REQUIRED_VARIABLES = Map.of(
            "DB_URL", "jdbc:mysql://db.internal:3306/udyamsetu_db",
            "DB_USERNAME", "udyamsetu_app",
            "DB_PASSWORD", "supplied-by-test-only",
            "APP_OWNER_USERNAME", "owner",
            "APP_OWNER_PASSWORD_HASH", "$2a$10$" + "a".repeat(53),
            "APP_ALLOWED_ORIGIN", "https://app.example.com");

    @Test
    void productionAllowedOriginIsOnlyFromTheEnvironment() throws IOException {
        PropertySource<?> prod = yamlSource("application-prod");
        assertEquals("${APP_ALLOWED_ORIGIN}", prod.getProperty(ALLOWED_ORIGIN_KEY));

        PropertyResolverBuilder resolver = new PropertyResolverBuilder().withProdProfile();
        assertMissing(resolver, ALLOWED_ORIGIN_KEY, "APP_ALLOWED_ORIGIN");

        String origin = new PropertyResolverBuilder().withProdProfile()
                .withVariables(REQUIRED_VARIABLES).resolver().getRequiredProperty(ALLOWED_ORIGIN_KEY);
        assertEquals("https://app.example.com", origin);
    }

    @Test
    void localConfigImportIsActiveOnlyForNonProductionProfiles() throws IOException {
        List<PropertySource<?>> documents = yamlDocuments("application");

        List<PropertySource<?>> importing = documents.stream()
                .filter(doc -> doc.containsProperty(LOCAL_IMPORT_KEY))
                .toList();
        assertEquals(1, importing.size(), "exactly one document may import the local configuration");

        PropertySource<?> importer = importing.get(0);
        assertEquals("!prod", importer.getProperty(ON_PROFILE_KEY),
                "the local import must be active only when the prod profile is NOT active");
        assertEquals("optional:file:./config/local.yaml", importer.getProperty(LOCAL_IMPORT_KEY));
    }

    @Test
    void theUnconditionalDocumentNeverImportsTheLocalFile() throws IOException {
        PropertySource<?> unconditional = yamlDocuments("application").get(0);

        assertFalse(unconditional.containsProperty(LOCAL_IMPORT_KEY));
        assertFalse(unconditional.containsProperty(ON_PROFILE_KEY),
                "the first document must apply to every profile and must not be activated conditionally");
    }

    @Test
    void productionProfileDefinesNoLocalImportAndNoLocalCredentials() throws IOException {
        PropertySource<?> prod = yamlSource("application-prod");

        assertFalse(prod.containsProperty(LOCAL_IMPORT_KEY), "the prod profile must not import config/local.yaml");
        assertFalse(prod.containsProperty("spring.config.activate.on-profile"));
    }

    @Test
    void publicErrorResponsesNeverIncludeInternals() throws IOException {
        PropertySource<?> shared = yamlSource("application");

        assertEquals("never", String.valueOf(shared.getProperty("server.error.include-stacktrace")));
        assertEquals("never", String.valueOf(shared.getProperty("server.error.include-message")));
        assertEquals("never", String.valueOf(shared.getProperty("server.error.include-binding-errors")));
        assertEquals("false", String.valueOf(shared.getProperty("server.error.include-exception")));
    }

    @Test
    void developmentAllowedOriginMatchesTheViteDevelopmentServer() throws IOException {
        PropertySource<?> shared = yamlSource("application");

        assertEquals("http://localhost:5173", shared.getProperty(ALLOWED_ORIGIN_KEY));
    }

    private static List<PropertySource<?>> yamlDocuments(String name) throws IOException {
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
                .load(name, new ClassPathResource(name + ".yaml"));
        assertTrue(!documents.isEmpty(), name + ".yaml must contain at least one document");
        return documents;
    }

    @Test
    void everySecretAndEnvironmentValueIsAPlaceholderWithNoDefault() throws IOException {
        PropertySource<?> prod = yamlSource("application-prod");

        assertEquals("${DB_URL}", prod.getProperty(DB_URL_KEY));
        assertEquals("${DB_USERNAME}", prod.getProperty(DB_USERNAME_KEY));
        assertEquals("${DB_PASSWORD}", prod.getProperty(DB_PASSWORD_KEY));
        assertEquals("${APP_OWNER_USERNAME}", prod.getProperty(OWNER_USERNAME_KEY));
        assertEquals("${APP_OWNER_PASSWORD_HASH}", prod.getProperty(OWNER_HASH_KEY));
    }

    @Test
    void missingEnvironmentVariablesFailResolutionAndNeverFallBack() throws IOException {
        PropertyResolverBuilder resolver = new PropertyResolverBuilder().withProdProfile();

        assertMissing(resolver, DB_URL_KEY, "DB_URL");
        assertMissing(resolver, DB_USERNAME_KEY, "DB_USERNAME");
        assertMissing(resolver, DB_PASSWORD_KEY, "DB_PASSWORD");
        assertMissing(resolver, OWNER_USERNAME_KEY, "APP_OWNER_USERNAME");
        assertMissing(resolver, OWNER_HASH_KEY, "APP_OWNER_PASSWORD_HASH");
    }

    @Test
    void suppliedVariablesResolveToTheDedicatedApplicationAccount() throws IOException {
        PropertyResolverBuilder resolver = new PropertyResolverBuilder()
                .withProdProfile()
                .withVariables(REQUIRED_VARIABLES);

        String username = resolver.resolver().getRequiredProperty(DB_USERNAME_KEY);

        assertEquals("udyamsetu_app", username);
        assertTrue(!username.equalsIgnoreCase("root"), "production must never use the root account");
        assertEquals("jdbc:mysql://db.internal:3306/udyamsetu_db",
                resolver.resolver().getRequiredProperty(DB_URL_KEY));
    }

    @Test
    void productionSettingsAreHardened() throws IOException {
        PropertySource<?> prod = yamlSource("application-prod");

        assertEquals("validate", prod.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", String.valueOf(prod.getProperty("spring.jpa.show-sql")));
        assertEquals("false", String.valueOf(prod.getProperty("spring.jmx.enabled")));
        assertEquals("127.0.0.1", prod.getProperty("server.address"));
        assertEquals("native", prod.getProperty("server.forward-headers-strategy"));
        assertEquals("true", String.valueOf(prod.getProperty("server.servlet.session.cookie.secure")));
    }

    @Test
    void sharedConfigurationHasEightHourSessionAndSecureCookieAttributes() throws IOException {
        PropertySource<?> shared = yamlSource("application");

        assertEquals("8h", shared.getProperty("server.servlet.session.timeout"));
        assertEquals("true", String.valueOf(shared.getProperty("server.servlet.session.cookie.http-only")));
        assertEquals("lax", shared.getProperty("server.servlet.session.cookie.same-site"));
    }

    @Test
    void sharedConfigurationNoLongerContainsDatabaseCredentials() throws IOException {
        PropertySource<?> shared = yamlSource("application");

        assertNull(shared.getProperty(DB_URL_KEY), "the local URL lives in config/local.yaml");
        assertNull(shared.getProperty(DB_USERNAME_KEY), "the local account lives in config/local.yaml");
        assertNull(shared.getProperty(DB_PASSWORD_KEY), "the local password lives in config/local.yaml");
    }

    private static void assertMissing(PropertyResolverBuilder builder, String key, String variable) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> builder.resolver().getRequiredProperty(key),
                "a missing " + variable + " must fail rather than fall back");
        assertTrue(error.getMessage().contains(variable), "the failure should name the missing variable");
    }

    private static PropertySource<?> yamlSource(String name) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(name, new ClassPathResource(name + ".yaml"));
        assertTrue(!sources.isEmpty(), name + ".yaml must contain at least one document");
        return sources.get(0);
    }

    /** Builds a resolver from the YAML files and only the variables a test explicitly supplies. */
    private static final class PropertyResolverBuilder {

        private final MutablePropertySources sources = new MutablePropertySources();

        PropertyResolverBuilder withProdProfile() throws IOException {
            for (PropertySource<?> source : new YamlPropertySourceLoader()
                    .load("application-prod", new ClassPathResource("application-prod.yaml"))) {
                sources.addLast(source);
            }
            return this;
        }

        PropertyResolverBuilder withVariables(Map<String, String> variables) {
            sources.addFirst(new MapPropertySource("supplied-variables", Map.copyOf(variables)));
            return this;
        }

        PropertySourcesPropertyResolver resolver() {
            return new PropertySourcesPropertyResolver(sources);
        }
    }
}
