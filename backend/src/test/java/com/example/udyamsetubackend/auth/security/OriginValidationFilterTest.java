package com.example.udyamsetubackend.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Origin and Fetch Metadata decisions for unsafe methods. Database-free: the filter is called directly with
 * mock requests, and the chain records whether the request was passed on.
 */
class OriginValidationFilterTest {

    private static final String PRODUCTION = "https://app.example.com";
    private static final String LOCAL = "http://localhost:5173";

    private final JsonSecurityErrorHandler errors = new JsonSecurityErrorHandler();

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void sameOriginUnsafeRequestPassesToTheChain(String method) throws Exception {
        MockHttpServletResponse response = filter(PRODUCTION, request(method, PRODUCTION, "same-origin"));

        assertEquals(200, response.getStatus());
        assertTrue(lastChainPassed, "the request must reach the rest of the chain");
    }

    @Test
    void siblingSubdomainOriginIsRejected() throws Exception {
        MockHttpServletResponse response = filter(PRODUCTION, request("POST", "https://sibling.example.com", null));

        assertRejected(response);
    }

    @Test
    void otherSubdomainOfTheApplicationIsRejected() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", "https://evil.app.example.com", null)));
    }

    @Test
    void schemeDowngradeIsRejectedBecauseTheOriginMustMatchExactly() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", "http://app.example.com", null)));
    }

    @Test
    void portDifferenceIsRejected() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", "https://app.example.com:8443", null)));
    }

    @Test
    void literalNullOriginIsRejected() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", "null", null)));
    }

    @Test
    void crossSiteFetchMetadataIsRejectedEvenWithTheAllowedOrigin() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", PRODUCTION, "cross-site")));
    }

    @Test
    void crossSiteFetchMetadataIsRejectedWithoutAnOriginHeader() throws Exception {
        assertRejected(filter(PRODUCTION, request("DELETE", null, "cross-site")));
    }

    @Test
    void sameSiteWithoutAnOriginIsRejected() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", null, "same-site")));
    }

    @Test
    void requestWithoutOriginOrFetchMetadataIsAllowedAsANonBrowserClient() throws Exception {
        MockHttpServletResponse response = filter(PRODUCTION, request("POST", null, null));

        assertEquals(200, response.getStatus());
        assertTrue(lastChainPassed);
    }

    @Test
    void developmentOriginIsAcceptedWhenConfiguredForDevelopment() throws Exception {
        filter(LOCAL, request("POST", LOCAL, "same-origin"));

        assertTrue(lastChainPassed);
    }

    @Test
    void developmentOriginIsRejectedInProduction() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", LOCAL, "same-origin")));
    }

    @Test
    void productionOriginIsRejectedInDevelopment() throws Exception {
        assertRejected(filter(LOCAL, request("POST", PRODUCTION, "same-origin")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void safeMethodsAreNotChecked(String method) throws Exception {
        MockHttpServletResponse response = filter(PRODUCTION, request(method, "https://sibling.example.com", "cross-site"));

        assertEquals(200, response.getStatus());
        assertTrue(lastChainPassed);
    }

    @Test
    void rejectionResponseIsGenericAndDoesNotEchoTheOrigin() throws Exception {
        String hostile = "https://attacker-controlled.example.net";

        MockHttpServletResponse response = filter(PRODUCTION, request("POST", hostile, null));

        assertRejected(response);
        String body = response.getContentAsString();
        assertFalse(body.contains("attacker-controlled"), "the rejected origin must not be echoed");
        assertFalse(body.contains(PRODUCTION), "the allowed origin must not be disclosed");
    }

    @Test
    void comparisonIsCaseSensitiveBecauseBrowsersSendLowercaseOrigins() throws Exception {
        assertRejected(filter(PRODUCTION, request("POST", "https://APP.example.com", null)));
    }

    // ------------------------------------------------------------------ helpers

    private boolean lastChainPassed;

    /** Runs the filter. Records whether the request reached the chain, and returns the response. */
    private MockHttpServletResponse filter(String allowedOrigin, MockHttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {
        OriginValidationFilter filter = new OriginValidationFilter(allowedOrigin, errors);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        lastChainPassed = false;

        filter.doFilter(request, response, chain);

        lastChainPassed = chain.getRequest() != null;
        return response;
    }

    private static void assertRejected(MockHttpServletResponse response) throws IOException {
        assertEquals(403, response.getStatus());
        assertNull(response.getForwardedUrl(), "a rejected request must not be forwarded");
        assertNotNull(response.getContentAsString());
    }

    private static MockHttpServletRequest request(String method, String origin, String fetchSite) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/customers/1");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        if (fetchSite != null) {
            request.addHeader("Sec-Fetch-Site", fetchSite);
        }
        return request;
    }
}
