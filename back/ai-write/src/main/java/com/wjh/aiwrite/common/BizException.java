package com.wjh.aiwrite.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常：code 直接对应 HTTP 状态码，前端按 code 分支处理
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(HttpStatus status, String message) {
        super(message);
        this.code = status.value();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(HttpStatus.BAD_REQUEST, message);
    }

    public static BizException notFound(String message) {
        return new BizException(HttpStatus.NOT_FOUND, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN, message);
    }

    /** 乐观锁冲突：正文已被其他操作更新 */
    public static BizException conflict(String message) {
        return new BizException(HttpStatus.CONFLICT, message);
    }

    /** 幂等冲突：已有进行中的任务 */
    public static BizException tooEarly(String message) {
        return new BizException(425, message);
    }
}
