package com.example.think.config;

import com.example.think.security.JwtAuthenticationFilter;
import com.example.think.security.JwtTokenProvider;
import com.example.think.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/user", "/api/user/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/classroom/assignments/*/submit").authenticated()
                .requestMatchers("/api/classroom/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(tokenProvider, blacklistedTokenRepository), 
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 특정 출처만 허용
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5500",
            "http://localhost:8080",
            "http://127.0.0.1:5500",
            "http://127.0.0.1:8080"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", 
            "POST", 
            "PUT", 
            "DELETE", 
            "OPTIONS"
        ));
        
        // 인증 정보 허용
        configuration.setAllowCredentials(true);
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type"
        ));
        
        // preflight 요청의 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) 
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 