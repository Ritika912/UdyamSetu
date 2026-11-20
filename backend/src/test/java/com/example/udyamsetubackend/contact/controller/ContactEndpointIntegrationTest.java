package com.example.udyamsetubackend.contact.controller;

import com.example.udyamsetubackend.auth.config.SecurityConfig;
import com.example.udyamsetubackend.auth.controller.AuthController;
import com.example.udyamsetubackend.auth.service.AuthService;
import com.example.udyamsetubackend.auth.service.LoginAttemptService;
import com.example.udyamsetubackend.common.exception.GlobalExceptionHandler;
import com.example.udyamsetubackend.contact.config.ContactConfiguration;
import com.example.udyamsetubackend.contact.service.ContactEmailSender;
import com.example.udyamsetubackend.contact.service.ContactService;
import com.example.udyamsetubackend.contact.security.ContactRateLimiter;
import com.example.udyamsetubackend.enquiry.controller.EnquiryController;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end behaviour of {@code POST /api/contact} through the real filter chain: body limit, rate limit,
 * Spring Security (CSRF and Origin), validation, honeypot, delivery and error responses.
 *
 * <p>SMTP is a Mockito mock of {@link JavaMailSender}. Nothing is sent over the network and no environment
 * variables are needed. The context is rebuilt for every test, so the rate-limit state starts empty.
 */
class ContactEndpointIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String ALLOWED_ORIGIN = "https://app.example.com";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String OWNER = "owner-it";
    private static final String PASSWORD = "integration-test-password";
    private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    private static final String SUPPORT = "support@getudyamsetu.com";
    private static final String VISITOR_EMAIL = "visitor@example.com";
    private static final String ACCEPTED = "Thank you. Your message has been sent to our support team.";
    private static final String SMTP_SECRET = "smtp-secret-do-not-leak";
    private static final String VALID_MESSAGE = "Hello, I would like a quote for printing services.";

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private MockMvc mvc;
    private JavaMailSender mailSender;
    private EnquiryService enquiryService;

    private record Visitor(MockHttpSession session, String csrf) {
    }

    @BeforeEach
    void startFreshContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("it-contact", Map.of(
                "app.security.owner.username", OWNER,
                "app.security.owner.password-hash", HASH,
                "app.security.allowed-origin", ALLOWED_ORIGIN)));
        context.register(IntegrationTestConfiguration.class);
        context.refresh();

        mailSender = context.getBean(JavaMailSender.class);
        enquiryService = context.getBean(EnquiryService.class);
        // Stubbed so the real MimeMessageHelper runs. Nothing is connected to, and send() is recorded only.
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(
                new java.util.Properties())));

        Filter[] contactFilters = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                .sorted(Comparator.comparingInt(FilterRegistrationBean::getOrder))
                .map(FilterRegistrationBean::getFilter)
                .toArray(Filter[]::new);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(contactFilters)
                .apply(springSecurity())
                .build();
    }

    // ------------------------------------------------------------------ success and delivery

    @Test
    void validSubmissionIsAcceptedWith202AndDeliveredOnceToTheSupportMailbox() throws Exception {
        Visitor visitor = anonymousVisitor();

        MvcResult result = submit(visitor, validPayload())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(ACCEPTED))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(SMTP_SECRET));
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertEquals(SUPPORT, ((InternetAddress) sent.getFrom()[0]).getAddress());
        assertEquals(SUPPORT, ((InternetAddress) sent.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress());
        assertEquals(VISITOR_EMAIL, ((InternetAddress) sent.getReplyTo()[0]).getAddress());
    }

    @Test
    void visitorAddressNeverBecomesTheSender() throws Exception {
        submit(anonymousVisitor(), validPayload()).andExpect(status().isAccepted());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals(SUPPORT, ((InternetAddress) captor.getValue().getFrom()[0]).getAddress());
    }

    // ------------------------------------------------------------------ security (CSRF, Origin, authentication)

    @Test
    void anonymousSubmissionWithoutCsrfTokenIsRefusedAndNothingIsSent() throws Exception {
        mvc.perform(withOrigin(post("/api/contact")).contentType(JSON).content(validPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(mailSender);
    }

    @Test
    void submissionWithTokenFromAnotherSessionIsRefused() throws Exception {
        Visitor first = anonymousVisitor();
        Visitor second = anonymousVisitor();

        mvc.perform(withOrigin(post("/api/contact").session(second.session()).header(CSRF_HEADER, first.csrf()))
                        .contentType(JSON).content(validPayload()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(mailSender);
    }

    @Test
    void wrongOriginIsRefusedEvenWithAValidToken() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(post("/api/contact").session(visitor.session()).header(CSRF_HEADER, visitor.csrf())
                        .header("Origin", "https://evil.example.org")
                        .contentType(JSON).content(validPayload()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(mailSender);
    }

    @Test
    void crossSiteFetchMetadataIsRefusedEvenWithTheAllowedOrigin() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(post("/api/contact").session(visitor.session()).header(CSRF_HEADER, visitor.csrf())
                        .header("Origin", ALLOWED_ORIGIN).header("Sec-Fetch-Site", "cross-site")
                        .contentType(JSON).content(validPayload()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(mailSender);
    }

    @Test
    void onlyTheExactContactPostIsPublic() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(get("/api/contact")).andExpect(status().isUnauthorized());
        mvc.perform(withOrigin(put("/api/contact").session(visitor.session()).header(CSRF_HEADER, visitor.csrf()))
                        .contentType(JSON).content(validPayload()))
                .andExpect(status().isUnauthorized());
        mvc.perform(withOrigin(post("/api/contact/extra").session(visitor.session()).header(CSRF_HEADER, visitor.csrf()))
                        .contentType(JSON).content(validPayload()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mailSender);
    }

    @Test
    void existingBusinessApisStillRequireOwnerAuthentication() throws Exception {
        Visitor visitor = anonymousVisitor();

        mvc.perform(withOrigin(post("/api/enquiries").session(visitor.session()).header(CSRF_HEADER, visitor.csrf()))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(enquiryService);
    }

    // ------------------------------------------------------------------ validation and honeypot

    @Test
    void missingRequiredFieldsReturn400WithFieldErrorsAndSendNothing() throws Exception {
        Visitor visitor = anonymousVisitor();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "");
        payload.put("email", "not-an-email");
        payload.put("subject", "Quote");
        payload.put("message", VALID_MESSAGE);

        submit(visitor, json(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.fullName").value("fullName is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("email must be a valid email address"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void headerInjectionInSubjectIsRejectedBeforeAnyEmailIsBuilt() throws Exception {
        Visitor visitor = anonymousVisitor();
        Map<String, Object> payload = validFields();
        payload.put("subject", "Quote\r\nBcc: attacker@example.com");

        submit(visitor, json(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.subject").exists());

        verifyNoInteractions(mailSender);
    }

    @Test
    void populatedHoneypotIsRefusedWith400AndSendsNothingAndReportsNoSuccess() throws Exception {
        Visitor visitor = anonymousVisitor();
        Map<String, Object> payload = validFields();
        payload.put("website", "http://spam.example");

        MvcResult result = submit(visitor, json(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your message could not be accepted."))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(ACCEPTED));
        verifyNoInteractions(mailSender);
    }

    @Test
    void emptyHoneypotIsIgnored() throws Exception {
        Visitor visitor = anonymousVisitor();
        Map<String, Object> payload = validFields();
        payload.put("website", "");

        submit(visitor, json(payload)).andExpect(status().isAccepted());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ------------------------------------------------------------------ failure responses

    @Test
    void smtpFailureReturns503WithAFixedMessageAndNoSmtpDetail() throws Exception {
        doThrow(new MailSendException("535 auth failed user=" + SUPPORT + " pass=" + SMTP_SECRET))
                .when(mailSender).send(any(MimeMessage.class));
        Visitor visitor = anonymousVisitor();

        MvcResult result = submit(visitor, validPayload())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "We could not send your message right now. Please try again later."))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(SMTP_SECRET), "the SMTP password detail must never reach the client");
        assertFalse(body.contains("535"), "the SMTP reply must never reach the client");
        assertFalse(body.contains("MailSendException"), "the exception type must never reach the client");
        assertFalse(body.contains(SUPPORT), "no server or account detail may reach the client");
    }

    // ------------------------------------------------------------------ abuse controls

    @Test
    void fifthSubmissionFromOneClientIsAcceptedAndTheSixthIsRateLimited() throws Exception {
        Visitor visitor = anonymousVisitor();

        for (int i = 0; i < ContactRateLimiter.MAX_SUBMISSIONS; i++) {
            submit(visitor, validPayload()).andExpect(status().isAccepted());
        }
        mvc.perform(submitRequest(visitor, validPayload()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));

        verify(mailSender, times(ContactRateLimiter.MAX_SUBMISSIONS)).send(any(MimeMessage.class));
    }

    @Test
    void oversizedBodyIsRefusedWith413AndSendsNothing() throws Exception {
        Visitor visitor = anonymousVisitor();
        Map<String, Object> payload = validFields();
        payload.put("message", "a".repeat(17_000));

        mvc.perform(submitRequest(visitor, json(payload)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.status").value(413));

        verifyNoInteractions(mailSender);
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> validFields() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "Asha Verma");
        payload.put("email", VISITOR_EMAIL);
        payload.put("subject", "Quote request");
        payload.put("message", VALID_MESSAGE);
        return payload;
    }

    private static String validPayload() {
        return json(validFields());
    }

    private static String json(Map<String, Object> payload) {
        return JSON_MAPPER.writeValueAsString(payload);
    }

    private ResultActions submit(Visitor visitor, String body) throws Exception {
        return mvc.perform(submitRequest(visitor, body));
    }

    private static MockHttpServletRequestBuilder submitRequest(Visitor visitor, String body) {
        return withOrigin(post("/api/contact").session(visitor.session()).header(CSRF_HEADER, visitor.csrf()))
                .contentType(JSON)
                .content(body);
    }

    private Visitor anonymousVisitor() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.csrfToken");
        assertNotNull(token);
        return new Visitor((MockHttpSession) result.getRequest().getSession(false), token);
    }

    /** Marks a request as a same-origin browser request from the allowed origin. */
    private static MockHttpServletRequestBuilder withOrigin(MockHttpServletRequestBuilder builder) {
        return builder.header("Origin", ALLOWED_ORIGIN).header("Sec-Fetch-Site", "same-origin");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            AuthController.class,
            AuthService.class,
            LoginAttemptService.class,
            GlobalExceptionHandler.class,
            ContactController.class,
            ContactService.class,
            ContactEmailSender.class,
            ContactRateLimiter.class,
            ContactConfiguration.class,
            EnquiryController.class
    })
    static class IntegrationTestConfiguration {

        /** The only SMTP object in the context. It is a mock, so no connection is ever made. */
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        EnquiryService enquiryService() {
            return mock(EnquiryService.class);
        }
    }
}
