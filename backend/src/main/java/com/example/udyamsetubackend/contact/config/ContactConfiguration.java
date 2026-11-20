package com.example.udyamsetubackend.contact.config;

import com.example.udyamsetubackend.contact.security.ContactBodySizeFilter;
import com.example.udyamsetubackend.contact.security.ContactRateLimitFilter;
import com.example.udyamsetubackend.contact.security.ContactRateLimiter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the contact abuse controls as servlet filters that run before Spring Security, so oversized bodies and
 * rate-limited clients are refused before any authentication or parsing work.
 *
 * <p>Both filters match only the exact path {@code /api/contact}. No other route is affected.
 */
@Configuration(proxyBeanMethods = false)
public class ContactConfiguration {

    static final String CONTACT_PATH = "/api/contact";
    /**
     * Near the very start of the chain, so both checks run before Spring Security. Its own order is not relied upon,
     * because the exact value depends on the Spring Boot version.
     */
    static final int BODY_SIZE_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    static final int RATE_LIMIT_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 11;

    @Bean
    FilterRegistrationBean<ContactBodySizeFilter> contactBodySizeFilter() {
        FilterRegistrationBean<ContactBodySizeFilter> registration =
                new FilterRegistrationBean<>(new ContactBodySizeFilter());
        registration.setName("contactBodySizeFilter");
        registration.addUrlPatterns(CONTACT_PATH);
        registration.setOrder(BODY_SIZE_FILTER_ORDER);
        return registration;
    }

    @Bean
    FilterRegistrationBean<ContactRateLimitFilter> contactRateLimitFilter(ContactRateLimiter rateLimiter) {
        FilterRegistrationBean<ContactRateLimitFilter> registration =
                new FilterRegistrationBean<>(new ContactRateLimitFilter(rateLimiter));
        registration.setName("contactRateLimitFilter");
        registration.addUrlPatterns(CONTACT_PATH);
        registration.setOrder(RATE_LIMIT_FILTER_ORDER);
        return registration;
    }
}
