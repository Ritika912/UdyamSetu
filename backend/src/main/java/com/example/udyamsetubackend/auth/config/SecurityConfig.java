package com.example.udyamsetubackend.auth.config;

import com.example.udyamsetubackend.auth.security.JsonSecurityErrorHandler;
import com.example.udyamsetubackend.auth.security.OriginValidationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.time.Clock;

/**
 * Default-deny security for the single owner account.
 *
 * <p>Public: {@code POST /api/auth/login}, {@code GET /api/auth/session}, {@code POST /api/contact} and {@code /error}. Everything else,
 * including {@code POST /api/auth/logout} and all business APIs, requires an authenticated owner session.
 *
 * <ul>
 *   <li>CSRF: synchronizer token stored in the HTTP session. It is returned in the body of the session and login
 *       responses and sent back in the {@code X-XSRF-TOKEN} header. No CSRF cookie exists.</li>
 *   <li>Origin: {@link OriginValidationFilter} runs before the CSRF filter on unsafe methods, and rejects
 *       cross-site requests and any origin other than {@code app.security.allowed-origin}.</li>
 *   <li>Sessions: server-side {@code HttpSession}, with its ID changed at login (see {@code AuthController}).
 *       The 8-hour idle timeout and cookie flags come from {@code application*.yaml}.</li>
 *   <li>Errors: 401 and 403 use {@link JsonSecurityErrorHandler}.</li>
 *   <li>No method-level security is used, so access-denied errors never reach {@code GlobalExceptionHandler}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({OwnerAccountProperties.class, AllowedOriginProperties.class})
public class SecurityConfig {

    /** Request header that carries the CSRF token. The frontend must send it on unsafe requests. */
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CsrfTokenRepository csrfTokenRepository,
                                            AllowedOriginProperties allowedOrigin)
            throws Exception {
        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler();

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/session").permitAll()
                        // Public contact form. Only this exact route is open; CSRF and origin checks still apply.
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new OriginValidationFilter(allowedOrigin.allowedOrigin(), errorHandler), CsrfFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // Login and logout are implemented in AuthController. Spring's form, basic and logout
                // filters are disabled so that logout is protected by the same rules as the business APIs.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The only user. The stored value is the BCrypt hash from configuration, never a plaintext password. */
    @Bean
    UserDetailsService ownerUserDetailsService(OwnerAccountProperties owner) {
        return new InMemoryUserDetailsManager(
                User.withUsername(owner.username())
                        .password(owner.passwordHash())
                        .roles("OWNER")
                        .build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService ownerUserDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(ownerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Session-backed CSRF token repository. The token is stored in the HTTP session and never placed in a cookie.
     * The header name is fixed to {@value #CSRF_HEADER_NAME}, which the frontend uses.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName(CSRF_HEADER_NAME);
        return repository;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Clock for login throttling. Injected so tests can control time. */
    @Bean
    Clock authClock() {
        return Clock.systemDefaultZone();
    }
}
