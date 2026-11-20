package com.example.udyamsetubackend.auth;

import com.example.udyamsetubackend.auth.config.SecurityConfig;
import com.example.udyamsetubackend.auth.controller.AuthController;
import com.example.udyamsetubackend.auth.service.AuthService;
import com.example.udyamsetubackend.auth.service.LoginAttemptService;
import com.example.udyamsetubackend.catalog.controller.ServiceOfferingController;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.common.exception.GlobalExceptionHandler;
import com.example.udyamsetubackend.customer.controller.CustomerController;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.dashboard.controller.DashboardController;
import com.example.udyamsetubackend.dashboard.service.DashboardService;
import com.example.udyamsetubackend.enquiry.controller.EnquiryController;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import com.example.udyamsetubackend.insights.controller.BusinessInsightsController;
import com.example.udyamsetubackend.insights.controller.OpportunityInsightsController;
import com.example.udyamsetubackend.insights.service.BusinessInsightsService;
import com.example.udyamsetubackend.insights.service.OpportunityInsightsService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security behaviour of the real filter chain: origin validation, session-bound CSRF, session rotation, logout,
 * throttling, default-deny authorization and error responses.
 *
 * <p>The context is built directly, so no environment post-processor runs, no database is involved and no test
 * environment variables are needed. Business services are mocks.
 */
class AuthSecurityIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String OWNER = "owner-it";
    private static final String PASSWORD = "integration-test-password";
    private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    /** Production-style origin for this context. Local development is tested separately. */
    private static final String ALLOWED_ORIGIN = "https://app.example.com";
    private static final String SIBLING_ORIGIN = "https://sibling.example.com";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String SESSION_COOKIE = "JSESSIONID";
    private static final String GENERIC_LOGIN_FAILURE = "Invalid username or password.";
    private static final String FORBIDDEN_MESSAGE = "Access denied. A valid session and security token are required.";
    /** A value that must never appear in any public response. */
    private static final String SECRET = "internal-secret-do-not-leak";

    private MockMvc mvc;
    private CustomerService customerService;

    /** A visitor's server-side session and the CSRF token that session holds. */
    private record Visitor(MockHttpSession session, String csrf) {
    }

    @BeforeEach
    void startFreshContext() {
        // A fresh context per test, so login-throttling state never leaks between tests.
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("it-owner", Map.of(
                "app.security.owner.username", OWNER,
                "app.security.owner.password-hash", HASH,
                "app.security.allowed-origin", ALLOWED_ORIGIN)));
        context.register(IntegrationTestConfiguration.class);
        context.refresh();

        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customerService = context.getBean(CustomerService.class);
    }

    // ------------------------------------------------------------------ session check and token issuance

    @Test
    void sessionCheckIsPublicAndReturnsAnAnonymousTokenInTheBody() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andExpect(jsonPath("$.csrfHeaderName").value(CSRF_HEADER));
    }

    @Test
    void noCsrfCookieIsEverIssued() throws Exception {
        MvcResult session = mvc.perform(get("/api/auth/session")).andReturn();
        MvcResult failedLogin = loginWith(null, null, OWNER, "wrong").andReturn();

        assertNoCsrfCookie(session);
        assertNoCsrfCookie(failedLogin);
    }

    @Test
    void tokenIsNeverPlacedInASetCookieHeader() throws Exception {
        MvcResult session = mvc.perform(get("/api/auth/session")).andReturn();
        String token = tokenOf(session);

        for (String header : session.getResponse().getHeaders(HttpHeaders.SET_COOKIE)) {
            assertFalse(header.contains(token), "the CSRF token must never appear in a cookie");
        }
    }

    @Test
    void tokenIsStableAcrossPageReloadsWithinTheSession() throws Exception {
        Visitor visitor = anonymousVisitor();

        MvcResult reload = mvc.perform(get("/api/auth/session").session(visitor.session())).andReturn();

        assertEquals(visitor.csrf(), tokenOf(reload));
    }

    @Test
    void reloadAfterLoginReturnsTheSignedInStateAndTheCurrentToken() throws Exception {
        Visitor owner = signIn();

        mvc.perform(get("/api/auth/session").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(OWNER))
                .andExpect(jsonPath("$.csrfToken").value(owner.csrf()));
    }

    @Test
    void sessionCheckIgnoresAnyOriginHeader() throws Exception {
        mvc.perform(get("/api/auth/session").header("Origin", SIBLING_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void responsesCarryTheDefaultSecurityHeaders() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ------------------------------------------------------------------ default-deny

    static Stream<Arguments> businessEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/customers"),
                Arguments.of("POST", "/api/customers"),
                Arguments.of("GET", "/api/customers/1"),
                Arguments.of("PUT", "/api/customers/1"),
                Arguments.of("DELETE", "/api/customers/1"),
                Arguments.of("GET", "/api/customers/1/enquiries"),
                Arguments.of("GET", "/api/enquiries"),
                Arguments.of("POST", "/api/enquiries"),
                Arguments.of("GET", "/api/enquiries/1"),
                Arguments.of("PATCH", "/api/enquiries/1/status"),
                Arguments.of("GET", "/api/services"),
                Arguments.of("POST", "/api/services"),
                Arguments.of("GET", "/api/services/1"),
                Arguments.of("PUT", "/api/services/1"),
                Arguments.of("DELETE", "/api/services/1"),
                Arguments.of("GET", "/api/dashboard"),
                Arguments.of("GET", "/api/insights/business"),
                Arguments.of("GET", "/api/insights/opportunities"));
    }

    @ParameterizedTest
    @MethodSource("businessEndpoints")
    void everyBusinessEndpointRejectsAnonymousCallersWith401Json(String method, String path) throws Exception {
        Visitor anonymous = anonymousVisitor();

        mvc.perform(withOrigin(sessionAndToken(request(HttpMethod.valueOf(method), path), anonymous))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void unknownPathIsRejectedWith401ForAnonymousCallers() throws Exception {
        mvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousWriteWithoutSessionOrTokenIsRejectedWith403Json() throws Exception {
        mvc.perform(withOrigin(post("/api/customers")).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void authenticatedOwnerCanReadBusinessApis() throws Exception {
        Visitor owner = signIn();
        when(customerService.listCustomers()).thenReturn(List.of());

        mvc.perform(get("/api/customers").session(owner.session())).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ CSRF on login

    @Test
    void loginWithoutAnySessionOrTokenIsForbidden() throws Exception {
        loginWith(null, null, OWNER, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void loginWithoutCsrfHeaderIsForbiddenEvenWithASession() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), null, OWNER, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void loginWithTokenFromAnotherSessionIsForbidden() throws Exception {
        Visitor first = anonymousVisitor();
        Visitor second = anonymousVisitor();

        loginWith(second.session(), first.csrf(), OWNER, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void loginWithInvalidTokenIsForbidden() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), "not-the-token", OWNER, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordReturnsTheGenericUnauthorizedMessage() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), visitor.csrf(), OWNER, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_LOGIN_FAILURE));
    }

    @Test
    void unknownUsernameReturnsTheSameGenericMessage() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), visitor.csrf(), "someone-else", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_LOGIN_FAILURE));
    }

    @Test
    void validationErrorOnLoginBodyReturns400WithFieldErrors() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), visitor.csrf(), OWNER, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ------------------------------------------------------------------ login rotation

    @Test
    void validTokenAuthenticatesAndReturnsAFreshTokenInTheBody() throws Exception {
        Visitor visitor = anonymousVisitor();

        MvcResult result = loginWith(visitor.session(), visitor.csrf(), OWNER, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(OWNER))
                .andExpect(jsonPath("$.csrfHeaderName").value(CSRF_HEADER))
                .andReturn();

        assertNotEquals(visitor.csrf(), tokenOf(result), "login must issue a different token");
        assertNoCsrfCookie(result);
    }

    @Test
    void loginRotatesTheSessionIdentifier() throws Exception {
        Visitor visitor = anonymousVisitor();
        String before = visitor.session().getId();

        MvcResult result = loginWith(visitor.session(), visitor.csrf(), OWNER, PASSWORD)
                .andExpect(status().isOk()).andReturn();

        assertNotEquals(before, sessionOf(result).getId(), "session fixation: the id must change at login");
    }

    @Test
    void preLoginTokenIsRejectedAfterLogin() throws Exception {
        Visitor before = anonymousVisitor();
        MvcResult result = loginWith(before.session(), before.csrf(), OWNER, PASSWORD)
                .andExpect(status().isOk()).andReturn();
        Visitor afterLogin = new Visitor(sessionOf(result), tokenOf(result));

        // The post-login session with the pre-login token must be refused, even though the owner is signed in.
        mvc.perform(withOrigin(delete("/api/customers/1")
                        .session(afterLogin.session())
                        .header(CSRF_HEADER, before.csrf())))
                .andExpect(status().isForbidden());
        verify(customerService, never()).deleteCustomer(anyLong());
    }

    @Test
    void freshTokenFromLoginAuthorizesWrites() throws Exception {
        Visitor owner = signIn();

        mvc.perform(authorized(delete("/api/customers/1"), owner)).andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1L);
    }

    @Test
    void authenticatedWriteWithoutTokenIsRejectedWith403Json() throws Exception {
        Visitor owner = signIn();

        mvc.perform(withOrigin(delete("/api/customers/1").session(owner.session())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(customerService, never()).deleteCustomer(anyLong());
    }

    @Test
    void authenticatedWriteWithInvalidTokenIsRejectedWith403() throws Exception {
        Visitor owner = signIn();

        mvc.perform(withOrigin(delete("/api/customers/1").session(owner.session()))
                        .header(CSRF_HEADER, "forged-token"))
                .andExpect(status().isForbidden());
        verify(customerService, never()).deleteCustomer(anyLong());
    }

    // ------------------------------------------------------------------ throttling

    @Test
    void fifthFailureLocksTheOwnerAndTheCorrectPasswordIsRefusedToo() throws Exception {
        Visitor visitor = anonymousVisitor();

        for (int i = 0; i < 5; i++) {
            loginWith(visitor.session(), visitor.csrf(), OWNER, "wrong-password")
                    .andExpect(status().isUnauthorized());
        }

        loginWith(visitor.session(), visitor.csrf(), OWNER, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    // ------------------------------------------------------------------ logout

    @Test
    void logoutRequiresAuthentication() throws Exception {
        Visitor anonymous = anonymousVisitor();

        mvc.perform(authorized(post("/api/auth/logout"), anonymous)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresACsrfTokenAndLeavesTheSessionIntact() throws Exception {
        Visitor owner = signIn();

        mvc.perform(withOrigin(post("/api/auth/logout").session(owner.session())))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/session").session(owner.session()))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void logoutInvalidatesTheSessionAndItsCsrfStateAndExpiresTheSessionCookie() throws Exception {
        Visitor owner = signIn();

        MvcResult logout = mvc.perform(authorized(post("/api/auth/logout"), owner))
                .andExpect(status().isNoContent())
                .andReturn();

        String sessionCookie = setCookieHeader(logout, SESSION_COOKIE);
        assertNotNull(sessionCookie, "the session cookie must be expired by logout");
        assertTrue(sessionCookie.contains("Max-Age=0"));
        assertNoCsrfCookie(logout);

        // The old session is gone: it reports anonymous with a new token, and the old token no longer works.
        MvcResult afterLogout = mvc.perform(get("/api/auth/session").session(owner.session()))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andReturn();
        assertNotEquals(owner.csrf(), tokenOf(afterLogout));

        mvc.perform(authorized(delete("/api/customers/1"), owner)).andExpect(status().isForbidden());
        verify(customerService, never()).deleteCustomer(anyLong());
    }

    // ------------------------------------------------------------------ origin and Fetch Metadata

    @Test
    void wrongOriginOnLoginIsRejectedWithTheGenericMessage() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content(credentials(OWNER, PASSWORD))
                        .session(visitor.session())
                        .header(CSRF_HEADER, visitor.csrf())
                        .header("Origin", "https://evil.example.org"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN_MESSAGE));
    }

    @Test
    void siblingSubdomainOriginIsRejectedForWrites() throws Exception {
        Visitor owner = signIn();

        mvc.perform(sessionAndToken(delete("/api/customers/1"), owner).header("Origin", SIBLING_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN_MESSAGE));

        verify(customerService, never()).deleteCustomer(anyLong());
    }

    @Test
    void responsesNeverEchoTheRejectedOrigin() throws Exception {
        Visitor owner = signIn();

        MvcResult rejected = mvc.perform(sessionAndToken(delete("/api/customers/1"), owner)
                        .header("Origin", SIBLING_ORIGIN))
                .andReturn();

        assertFalse(rejected.getResponse().getContentAsString().contains("sibling"));
    }

    @Test
    void localDevelopmentOriginIsRejectedWhenProductionOriginIsConfigured() throws Exception {
        Visitor owner = signIn();

        mvc.perform(sessionAndToken(delete("/api/customers/1"), owner).header("Origin", "http://localhost:5173"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossSiteFetchMetadataIsRejectedEvenWithTheAllowedOrigin() throws Exception {
        Visitor owner = signIn();

        mvc.perform(sessionAndToken(delete("/api/customers/1"), owner)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameOriginBrowserRequestsAreAccepted() throws Exception {
        Visitor visitor = anonymousVisitor();

        loginWith(visitor.session(), visitor.csrf(), OWNER, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void requestsWithoutBrowserMetadataAreStillSubjectToTokenAndAuthentication() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content(credentials(OWNER, PASSWORD))
                        .session(visitor.session())
                        .header(CSRF_HEADER, visitor.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void originIsNotCheckedForSafeMethods() throws Exception {
        mvc.perform(get("/api/auth/session").header("Origin", SIBLING_ORIGIN).header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    // ------------------------------------------------------------------ error responses and error dispatch

    @Test
    void anonymousErrorResponseExposesNoInternals() throws Exception {
        mvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void serverErrorReturnsOnlyTheGenericMessageAndNoInternals() throws Exception {
        Visitor owner = signIn();
        when(customerService.listCustomers()).thenThrow(new IllegalStateException(SECRET));

        MvcResult result = mvc.perform(get("/api/customers").session(owner.session()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(SECRET), "the internal exception message must never reach the client");
        assertFalse(body.contains("IllegalStateException"), "the exception type must never reach the client");
        assertFalse(body.contains("at com."), "no stack trace may reach the client");
    }

    @Test
    void errorDispatchToErrorPathIsPublicAndLeaksNothing() throws Exception {
        // Simulates the container's ERROR dispatch that follows an unhandled exception. The exception and its
        // message are attached, as a container would attach them, but must not appear in the public response.
        MvcResult result = mvc.perform(get("/error").with(errorDispatch(new IllegalStateException(SECRET))))
                .andReturn();

        assertTrue(result.getResponse().getStatus() != 401, "/error must be reachable without a session");
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(SECRET));
        assertFalse(body.contains("IllegalStateException"));
        assertFalse(body.contains("at com."));
    }

    // ------------------------------------------------------------------ helpers

    private Visitor anonymousVisitor() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andReturn();
        return new Visitor(sessionOf(result), tokenOf(result));
    }

    /** Signs the owner in through the real login endpoint and returns the post-login visitor. */
    private Visitor signIn() throws Exception {
        Visitor anonymous = anonymousVisitor();
        MvcResult result = loginWith(anonymous.session(), anonymous.csrf(), OWNER, PASSWORD)
                .andExpect(status().isOk())
                .andReturn();
        return new Visitor(sessionOf(result), tokenOf(result));
    }

    /** A login request from a browser on the allowed origin. Pass null for a missing session or token. */
    private ResultActions loginWith(MockHttpSession session, String csrfHeader, String username, String password)
            throws Exception {
        MockHttpServletRequestBuilder builder = withOrigin(post("/api/auth/login"))
                .contentType(JSON)
                .content(credentials(username, password));
        if (session != null) {
            builder.session(session);
        }
        if (csrfHeader != null) {
            builder.header(CSRF_HEADER, csrfHeader);
        }
        return mvc.perform(builder);
    }

    /** Adds the session and CSRF header of a visitor, without any origin headers. */
    private static MockHttpServletRequestBuilder sessionAndToken(MockHttpServletRequestBuilder builder, Visitor v) {
        return builder.session(v.session()).header(CSRF_HEADER, v.csrf());
    }

    /** Adds the session and CSRF header of a visitor, sent from a browser on the allowed origin. */
    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder, Visitor v) {
        return withOrigin(sessionAndToken(builder, v));
    }

    /** Marks a request as a same-origin browser request from the allowed origin. */
    private static MockHttpServletRequestBuilder withOrigin(MockHttpServletRequestBuilder builder) {
        return builder.header("Origin", ALLOWED_ORIGIN).header("Sec-Fetch-Site", "same-origin");
    }

    private static RequestPostProcessor errorDispatch(Throwable cause) {
        return request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute("jakarta.servlet.error.status_code", 500);
            request.setAttribute("jakarta.servlet.error.exception", cause);
            request.setAttribute("jakarta.servlet.error.message", cause.getMessage());
            return request;
        };
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String tokenOf(MvcResult result) throws Exception {
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.csrfToken");
        assertNotNull(token, "the response must carry a CSRF token");
        return token;
    }

    private static MockHttpSession sessionOf(MvcResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "the response must be associated with a server-side session");
        return session;
    }

    private static void assertNoCsrfCookie(MvcResult result) {
        for (String header : result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)) {
            assertFalse(header.startsWith("XSRF-TOKEN="), "no CSRF cookie may be issued: " + header);
        }
    }

    /** The full Set-Cookie header for a cookie, or null. */
    private static String setCookieHeader(MvcResult result, String name) {
        for (String header : result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)) {
            if (header.startsWith(name + "=")) {
                return header;
            }
        }
        return null;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            AuthService.class,
            LoginAttemptService.class,
            AuthController.class,
            GlobalExceptionHandler.class,
            CustomerController.class,
            EnquiryController.class,
            ServiceOfferingController.class,
            DashboardController.class,
            BusinessInsightsController.class,
            OpportunityInsightsController.class
    })
    static class IntegrationTestConfiguration {

        @Bean
        CustomerService customerService() {
            return mock(CustomerService.class);
        }

        @Bean
        EnquiryService enquiryService() {
            return mock(EnquiryService.class);
        }

        @Bean
        ServiceOfferingService serviceOfferingService() {
            return mock(ServiceOfferingService.class);
        }

        @Bean
        DashboardService dashboardService() {
            return mock(DashboardService.class);
        }

        @Bean
        BusinessInsightsService businessInsightsService() {
            return mock(BusinessInsightsService.class);
        }

        @Bean
        OpportunityInsightsService opportunityInsightsService() {
            return mock(OpportunityInsightsService.class);
        }
    }
}
