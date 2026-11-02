package com.wjh.aiwrite.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录凭证服务。
 *
 * MVP 使用「不透明 token + 进程内缓存」，重启后失效。
 * 后续若要改成 JWT / Redis，只需替换实现，controller 与前端都不受影响。
 */
public interface TokenService {

    /** 签发 token */
    String issue(User user);

    /** 解析 token -> userId */
    Optional<Long> resolve(String token);

    void revoke(String token);
}

@Service
class InMemoryTokenService implements TokenService {

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    @Override
    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, user.getId());
        return token;
    }

    @Override
    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokens.get(token.trim()));
    }

    @Override
    public void revoke(String token) {
        if (token != null) {
            tokens.remove(token.trim());
        }
    }
}
