package com.wjh.aiwrite.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个请求分配 requestId，并回填到响应头 X-Request-Id
 */
/** 注意：不能叫 requestContextFilter —— 会与 Spring Boot 内置的同名 bean 冲突 */
@Component("requestIdFilter")
@Order(-100)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString().substring(0, 18);
        RequestContext.set(id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}
