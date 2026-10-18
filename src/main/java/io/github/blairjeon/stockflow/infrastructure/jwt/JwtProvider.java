package io.github.blairjeon.stockflow.infrastructure.jwt;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(JwtUserInfo userInfo){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessExpiration().toMillis());

        return Jwts.builder()
                .signWith(key)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .subject(String.valueOf(userInfo.userId()))
                .claim("UserRole", userInfo.role().name())
                .claim("type", TokenType.ACCESS.name())
                .compact();
    }

    public String createRefreshToken(Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshExpiration().toMillis());

        return Jwts.builder()
                .signWith(key)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .subject(String.valueOf(userId))
                .claim("type", TokenType.REFRESH.name())
                .compact();
    }

    public JwtUserInfo parseAccessToken(String token){
        Claims claims = parseAndValidate(token, TokenType.ACCESS);
        return new JwtUserInfo(
                Long.valueOf(claims.getSubject()),
                UserRole.valueOf(claims.get("UserRole", String.class))
        );
    }

    public Long parseRefreshToken(String token){
        Claims claims = parseAndValidate(token, TokenType.REFRESH);
        return Long.valueOf(claims.getSubject());
    }


    private Claims parseAndValidate(String token, TokenType expectedType){
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        TokenType actualType = TokenType.valueOf(claims.get("type", String.class));

        if(!actualType.equals(expectedType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return claims;
    }
}