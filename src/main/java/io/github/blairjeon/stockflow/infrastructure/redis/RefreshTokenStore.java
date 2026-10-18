package io.github.blairjeon.stockflow.infrastructure.redis;

import io.github.blairjeon.stockflow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
    private static final String KEY_PREFIX = "auth:refresh";

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                        return 1
                    end
                    return 0
                    """, Long.class
            );

    public void save(Long userId, String token, Duration ttl){
        redisTemplate.opsForValue().set(key(userId), token, ttl);
    }

    public void delete(Long userId){
        redisTemplate.delete(key(userId));
    }

    public boolean rotate(Long userId, String oldToken, String newToken, Duration ttl){
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(userId)),
                oldToken,
                newToken,
                Long.toString(ttl.toMillis())
        );

        return Long.valueOf(1L).equals(result);
    }

    private String key(Long userId){
        return KEY_PREFIX + userId;
    }
}