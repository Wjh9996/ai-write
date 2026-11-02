package com.wjh.aiwrite.generation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接管理（基础设施层）
 *
 * 文档 §4.3：SSE 断线不等于任务停止 —— 断线只是移除本地 emitter，
 * 事件已经落库，客户端重连时用 Last-Event-ID 补读即可。
 */
@Slf4j
@Component
public class SseHub {

    private static final long TIMEOUT = 30 * 60 * 1000L;

    /** jobId -> 该任务的所有连接 */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(String jobId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(e -> remove(jobId, emitter));
        return emitter;
    }

    /** 广播一个事件给所有在线连接；返回是否至少有一个客户端收到 */
    public boolean send(String jobId, Object payload, Long eventId) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list == null || list.isEmpty()) {
            return false;
        }
        boolean any = false;
        for (SseEmitter e : list) {
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                        .data(payload)
                        .name("message");
                if (eventId != null) {
                    builder = builder.id(String.valueOf(eventId));
                }
                e.send(builder);
                any = true;
            } catch (IOException | IllegalStateException ex) {
                log.debug("[sse] send failed, jobId={} {}", jobId, ex.getMessage());
                remove(jobId, e);
            }
        }
        return any;
    }

    /** 是否已有客户端连上（用于避免流水线在连接建立前把开头事件发完） */
    public boolean hasConnections(String jobId) {
        List<SseEmitter> list = emitters.get(jobId);
        return list != null && !list.isEmpty();
    }

    /** 等待第一个连接建立，最多 waitMs 毫秒 */
    public void awaitFirstConnection(String jobId, long waitMs) {
        long deadline = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() < deadline) {
            if (hasConnections(jobId)) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void complete(String jobId) {
        List<SseEmitter> list = emitters.remove(jobId);
        if (list != null) {
            list.forEach(e -> {
                try {
                    e.complete();
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void remove(String jobId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(jobId);
            }
        }
    }
}
