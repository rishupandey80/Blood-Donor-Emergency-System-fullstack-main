package com.serverproject.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login",
                                "/api/auth/forgot-password", "/api/auth/reset-password",
                                "/api/auth/signup/send-otp", "/api/auth/signup/verify-otp").permitAll()

                        // Donor rules
                        .requestMatchers(HttpMethod.GET, "/api/donors/search", "/api/donors/search/bloodgroup", "/api/donors/search/city").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donors").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donors/add").hasRole("DONOR")
                        .requestMatchers(HttpMethod.PUT, "/api/donors/update-availability").hasAnyRole("DONOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donors/*/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donors/*/reject").hasRole("ADMIN")

                        // BloodRequest rules: requesters create and track only their own requests.
                        // Donors and verified blood banks receive the incoming request feed.
                        // Admin can view/manage all requests but cannot create or accept them.
                        .requestMatchers(HttpMethod.POST, "/api/request").hasAnyRole("USER", "HOSPITAL")
                        .requestMatchers(HttpMethod.GET, "/api/request/mine").hasAnyRole("USER", "HOSPITAL")
                        .requestMatchers(HttpMethod.GET, "/api/request/active").hasAnyRole("DONOR", "BLOOD_BANK")
                        .requestMatchers(HttpMethod.GET, "/api/request/accepted-by-me").hasAnyRole("DONOR", "BLOOD_BANK")
                        .requestMatchers(HttpMethod.GET, "/api/request").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/request/*/accept").hasAnyRole("DONOR", "BLOOD_BANK")

                        // Hospital rules
                        .requestMatchers(HttpMethod.GET, "/api/hospitals/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hospitals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hospitals/register").hasRole("HOSPITAL")
                        .requestMatchers(HttpMethod.PUT, "/api/hospitals/*/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/hospitals/*/reject").hasRole("ADMIN")

                        // BloodBank rules
                        .requestMatchers(HttpMethod.GET, "/api/bloodbanks/search", "/api/bloodbanks/stock/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bloodbanks").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bloodbanks/register").hasRole("BLOOD_BANK")
                        .requestMatchers(HttpMethod.PUT, "/api/bloodbanks/stock").hasRole("BLOOD_BANK")
                        .requestMatchers(HttpMethod.GET, "/api/bloodbanks/stock/mine").hasRole("BLOOD_BANK")
                        .requestMatchers(HttpMethod.PUT, "/api/bloodbanks/*/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/bloodbanks/*/reject").hasRole("ADMIN")

                        // Account rules
                        .requestMatchers("/api/users/**").authenticated()

                        // Admin rules (ban/unban, user list)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:4173",
                "http://localhost:8081"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}