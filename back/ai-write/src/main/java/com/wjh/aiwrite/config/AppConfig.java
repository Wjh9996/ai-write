package com.wjh.aiwrite.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 通用配置：CORS、异步线程池、AI HTTP 客户端
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AppConfig {

    /** 前端 dev server 与预览地址 */
    private static final List<String> ORIGINS = List.of(
            "http://127.0.0.1:5180",
            "http://localhost:5180",
            "http://127.0.0.1:5173",
            "http://localhost:5173"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(ORIGINS);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("X-Request-Id"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * 生成流水线专用线程池。
     * 生成任务是长耗时 + 阻塞式 SSE 输出，必须独立于 Tomcat 请求线程，否则会打满连接池。
     */
    @Bean("generationExecutor")
    public Executor generationExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(64);
        ex.setThreadNamePrefix("gen-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    /**
     * 事件落库专用单线程：SSE 事件需要持久化以支持 Last-Event-ID 补读，
     * 但不能让几百次 save 拖慢生成主循环。
     */
    @Bean("eventWriterExecutor")
    public Executor eventWriterExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(2000);
        ex.setThreadNamePrefix("evt-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    /** 调用大模型用的 HTTP 客户端 */
    @Bean("aiHttpClient")
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
