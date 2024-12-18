package com.example.think.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import com.example.think.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider tokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, BlacklistedTokenRepository blacklistedTokenRepository) {
        this.tokenProvider = tokenProvider;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            logger.debug("Processing request with JWT: {}", jwt != null ? "present" : "absent");

            if (StringUtils.hasText(jwt)) {
                // 블랙리스트 확인
                if (blacklistedTokenRepository != null && blacklistedTokenRepository.existsByToken(jwt)) {
                    logger.error("Blacklisted token attempted to access");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                    return;
                }

                if (tokenProvider.validateToken(jwt)) {
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    if (authentication != null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Set Authentication to security context for '{}', uri: {}", 
                            authentication.getName(), request.getRequestURI());
                    }
                } else {
                    logger.debug("Invalid JWT token");
                }
            } else {
                logger.debug("No JWT token found in request");
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 