package com.wjh.aiwrite.common;

/**
 * 统一响应体，对齐前端 ApiResult：{code,message,data,requestId}
 */
public record ApiResult<T>(int code, String message, T data, String requestId) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, RequestContext.requestId());
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(0, message, data, RequestContext.requestId());
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null, RequestContext.requestId());
    }
}
