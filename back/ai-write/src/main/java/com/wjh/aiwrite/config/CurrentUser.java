package com.wjh.aiwrite.config;

import com.wjh.aiwrite.auth.TokenService;
import com.wjh.aiwrite.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户上下文
 *
 * 规则⑧：所有写入必须做 user_id + novel_id 权限校验，统一走这里取 userId
 */
@Component
public class CurrentUser {

    private final TokenService tokenService;

    public CurrentUser(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw BizException.forbidden("未登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long uid) {
            return uid;
        }
        return Long.parseLong(String.valueOf(principal));
    }
}
