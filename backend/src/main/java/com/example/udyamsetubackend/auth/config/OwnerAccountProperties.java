package com.example.udyamsetubackend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Pattern;

/**
 * The single owner account, read from {@code app.security.owner.*}.
 *
 * <p>Only a BCrypt hash is accepted. There is no plaintext password property. Validation runs in the
 * constructor and throws fixed messages that never contain the configured values, so a mistakenly
 * supplied plaintext password cannot reach the startup log.
 */
@ConfigurationProperties("app.security.owner")
public record OwnerAccountProperties(String username, String passwordHash) {

    static final int MAX_USERNAME_LENGTH = 100;

    /** BCrypt: $2a$, $2b$ or $2y$, a two-digit cost of 04 to 31, then 53 characters of salt and hash. */
    private static final Pattern BCRYPT_HASH =
            Pattern.compile("^\\$2[aby]\\$(0[4-9]|[12]\\d|3[01])\\$[./A-Za-z0-9]{53}$");

    public OwnerAccountProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("app.security.owner.username is not set. "
                    + "Add the owner username to config/local.yaml (local) or APP_OWNER_USERNAME (production).");
        }
        if (username.length() > MAX_USERNAME_LENGTH || !username.equals(username.strip())) {
            throw new IllegalStateException("app.security.owner.username must be at most "
                    + MAX_USERNAME_LENGTH + " characters with no leading or trailing spaces.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalStateException("app.security.owner.password-hash is not set. "
                    + "Provide a BCrypt hash of the owner password. Do not provide the plain password.");
        }
        if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalStateException("app.security.owner.password-hash is not a valid BCrypt hash. "
                    + "It must start with $2a$, $2b$ or $2y$ and be 60 characters long. "
                    + "Provide a hash, never the plain password.");
        }
    }

    /** Masks the hash so it cannot leak through logs or diagnostics. */
    @Override
    public String toString() {
        return "OwnerAccountProperties[username=" + username + ", passwordHash=********]";
    }
}
