package com.example.udyamsetubackend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * The single origin the browser application is served from, read from {@code app.security.allowed-origin}.
 *
 * <p>The value must be an exact origin: {@code scheme://host[:port]}, lowercase, with no path, query, fragment,
 * user information or wildcard. {@code https} is required, except for {@code http} on localhost, which is used
 * for development. Errors never echo the configured value.
 */
@ConfigurationProperties("app.security")
public record AllowedOriginProperties(String allowedOrigin) {

    public AllowedOriginProperties {
        if (allowedOrigin == null || allowedOrigin.isBlank()) {
            throw new IllegalStateException("app.security.allowed-origin is not set. Set the exact origin the "
                    + "application is served from, for example https://app.example.com (production). "
                    + "There is no default.");
        }
        if (allowedOrigin.contains("*")) {
            throw new IllegalStateException("app.security.allowed-origin must not contain a wildcard.");
        }
        if (!allowedOrigin.equals(allowedOrigin.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("app.security.allowed-origin must be lowercase.");
        }

        URI uri;
        try {
            uri = new URI(allowedOrigin);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("app.security.allowed-origin is not a valid origin.");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalStateException("app.security.allowed-origin must be an absolute origin with a host.");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalStateException(
                    "app.security.allowed-origin must not contain user information, a query or a fragment.");
        }
        if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
            throw new IllegalStateException("app.security.allowed-origin must not contain a path or trailing slash.");
        }
        boolean localhost = host.equals("localhost") || host.equals("127.0.0.1");
        boolean secure = scheme.equals("https");
        boolean developmentHttp = scheme.equals("http") && localhost;
        if (!secure && !developmentHttp) {
            throw new IllegalStateException(
                    "app.security.allowed-origin must use https. Plain http is allowed only for localhost.");
        }
    }
}
