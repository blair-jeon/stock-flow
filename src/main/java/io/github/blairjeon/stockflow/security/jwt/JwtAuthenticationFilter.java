package io.github.blairjeon.stockflow.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String bearer = request.getHeader("Authorization");
        if(bearer==null || !bearer.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearer.substring(7);
        JwtUserInfo userInfo;

        try {
            userInfo = jwtProvider.parseAccessToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userInfo.userId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_"+userInfo.userRole().toString()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}