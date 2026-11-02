package com.wjh.aiwrite.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转成 ApiResult，保证前端永远拿到 {code,message,data,requestId}
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        log.warn("[biz] code={} msg={}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("参数校验失败");
        return ApiResult.fail(HttpStatus.BAD_REQUEST.value(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResult.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * 数据库唯一约束冲突。generation_jobs 的 uk_active_chapter 会在这里兜底：
     * 即使应用层被绕过，「一章一个活动任务」这条规则仍然成立。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResult<Void> handleDataConflict(DataIntegrityViolationException e) {
        String root = e.getMostSpecificCause().getMessage();
        if (root != null && root.contains("uk_active_chapter")) {
            return ApiResult.fail(HttpStatus.CONFLICT.value(), "本章已有进行中的生成任务，不能重复创建");
        }
        log.warn("[data-conflict] {}", root);
        return ApiResult.fail(HttpStatus.CONFLICT.value(), "数据约束冲突：" + root);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("[unexpected]", e);
        return ApiResult.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务端异常：" + e.getMessage());
    }
}
