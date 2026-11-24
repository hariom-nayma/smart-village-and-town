package com.smartvillage.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/error",
                                "/uploads/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/otp/**",
                                "/api/auth/customer-pin-login",
                                "/api/public/**",
                                "/api/transport/catalog",
                                "/api/services/catalog"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/checkout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/payment-submission").permitAll()
                        // Admin restricted endpoints
                        .requestMatchers("/api/admin/**", "/api/constituency/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "COMPLAINT_MANAGER", "MLA")
                        // Vendor restricted endpoints
                        .requestMatchers("/api/vendor/**").hasAnyRole("VENDOR", "ADMIN", "SUPER_ADMIN", "MLA")
                        // Delivery partner endpoints
                        .requestMatchers("/api/delivery/**").hasAnyRole("DELIVERY_AGENT", "ADMIN", "SUPER_ADMIN", "MLA")
                        // All other authenticated endpoints
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
