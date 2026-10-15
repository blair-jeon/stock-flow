package io.github.blairjeon.stockflow.security.jwt;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.common.error.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

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
        } catch (ExpiredJwtException e) {
            writeErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            writeErrorResponse(response, ErrorCode.INVALID_TOKEN);
            return;
        } catch (BusinessException e) {
            writeErrorResponse(response, e.getErrorCode());
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

    private void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = ErrorResponse.from(errorCode);
        objectMapper.writeValue(response.getWriter(), body);
    }
}