package com.example.udyamsetubackend.common.contact;

import java.util.regex.Pattern;

/**
 * Turns a user-entered contact number into the canonical key used to identify a customer.
 *
 * <p>Rules (single-business MVP):
 * <ul>
 *   <li>Spaces and hyphens are ignored.</li>
 *   <li>Indian mobile: 10 digits starting with 6-9 (for example {@code 9876543210}), optionally written
 *       with the country code {@code +91} or {@code 91} as 12 digits. Key = the 10-digit number.</li>
 *   <li>Other numbers must start with {@code +} followed by 7 to 15 digits, and must not use the +91 code.
 *       Key = {@code +} followed by the digits, so they can never collide with an Indian key.</li>
 *   <li>Anything else is rejected. Without a {@code +}, only the Indian formats above are accepted, so a
 *       foreign number can never be mistaken for an Indian one.</li>
 * </ul>
 */
public final class ContactNumberNormalizer {

    public static final String INVALID_MESSAGE =
            "contactNumber must be a 10-digit Indian mobile number starting with 6-9, "
                    + "or an international number starting with '+' followed by 7 to 15 digits";

    private static final String INDIAN_COUNTRY_CODE = "91";
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    private static final Pattern INDIAN_MOBILE = Pattern.compile("[6-9][0-9]{9}");
    private static final Pattern INTERNATIONAL_DIGITS = Pattern.compile("[0-9]{7,15}");

    private ContactNumberNormalizer() {
    }

    /**
     * @return the canonical contact key
     * @throws IllegalArgumentException if the number is missing, malformed, or ambiguous
     */
    public static String normalize(String rawContactNumber) {
        if (rawContactNumber == null) {
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }
        String compact = SEPARATORS.matcher(rawContactNumber.trim()).replaceAll("");
        boolean hasPlus = compact.startsWith("+");
        String digits = hasPlus ? compact.substring(1) : compact;

        if (!DIGITS.matcher(digits).matches()) {
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }

        if (hasPlus) {
            if (digits.startsWith(INDIAN_COUNTRY_CODE)) {
                return indianKeyWithCountryCode(digits);
            }
            if (INTERNATIONAL_DIGITS.matcher(digits).matches()) {
                return "+" + digits;
            }
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }

        if (digits.length() == 10 && INDIAN_MOBILE.matcher(digits).matches()) {
            return digits;
        }
        if (digits.length() == 12 && digits.startsWith(INDIAN_COUNTRY_CODE)) {
            return indianKeyWithCountryCode(digits);
        }
        throw new IllegalArgumentException(INVALID_MESSAGE);
    }

    private static String indianKeyWithCountryCode(String digits) {
        // A number beginning with the Indian code is only valid as exactly 91 + a 10-digit mobile.
        String local = digits.length() == 12 ? digits.substring(2) : "";
        if (INDIAN_MOBILE.matcher(local).matches()) {
            return local;
        }
        throw new IllegalArgumentException(INVALID_MESSAGE);
    }
}
