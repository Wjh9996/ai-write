package com.wjh.aiwrite.common;

/**
 * 请求上下文：生成 requestId 并透传到响应体，便于前后端串联排查
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String id) {
        REQUEST_ID.set(id);
    }

    public static String requestId() {
        String id = REQUEST_ID.get();
        return id == null ? "-" : id;
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
