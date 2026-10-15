package io.github.blairjeon.stockflow.security.jwt;

import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createAccessToken(Long userId, UserRole userRole) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.accessExpiration().toMillis());

        return Jwts.builder()
                .signWith(key)
                .issuedAt(now)
                .expiration(expire)
                .subject(userId.toString())
                .claim("userRole", userRole.name())
                .claim("type", TokenType.ACCESS.toString())
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.refreshExpiration().toMillis());

        return Jwts.builder()
                .signWith(key)
                .issuedAt(now)
                .expiration(expire)
                .subject(userId.toString())
                .claim("type", TokenType.REFRESH.toString())
                .compact();
    }

    public JwtUserInfo parseAccessToken(String token){
        Claims claims = parseAndValidate(token, TokenType.ACCESS);
        return new JwtUserInfo(
                Long.valueOf(claims.getSubject()),
                UserRole.valueOf(claims.get("userRole", String.class))
        );
    }

    public Long parseRefreshToken(String token){
        Claims claims = parseAndValidate(token, TokenType.REFRESH);
        return Long.valueOf(claims.getSubject());
    }

    private Claims parseAndValidate(String token, TokenType expectedType){
        Claims claims =  Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        TokenType actualType = TokenType.valueOf(claims.get("type", String.class));

        if(expectedType!=actualType){
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return claims;
    }
}