# AI-Write（长篇小说 AI 创作台）项目长期笔记

## 技术栈
- 前端 `front/`：Vue 3.5 + TypeScript + Vite 6 + Pinia + Vue Router（hash），零 UI/图标/图表库，手写 SVG 图标与 Markdown 渲染。9 个页面。
- 后端 `back/ai-write/`：Spring Boot 4.0.8 + Java 21 + Spring Data JPA(Hibernate 7) + Spring Security + MySQL 8。严格 MVC：`controller → service → repository → model`，外加 dto / config / common / support。
- 持久层选 JPA 而非 MyBatis：MyBatis-Plus 3.5.x 的自动配置未适配 Spring Boot 4 / Spring Framework 7。
- 数据库用 **MySQL**（不是设计文档里写的 PostgreSQL）。库 `ai_write`，本机 root/123456（已写进 application.yml，上线前改环境变量 `MYSQL_PASSWORD`）。

## 关键约定
- MySQL 替代 PostgreSQL 的 partial unique index：用**虚拟生成列 + 唯一索引**（generation_jobs.active_key / uk_active_chapter），保证「一章只有一个活动任务」。
- 统一响应 `{code,message,data,requestId}`；异常用 BizException(code=HTTP 状态码)。
- SSE：SseEmitter + 事件落库（generation_events.event_seq 单调递增，支持 Last-Event-ID 补读）；事件落库走独立单线程，不能阻塞生成循环。
- 认证：Authorization: Bearer <token>，SSE 与文件下载用 ?token=<token>（EventSource / <a download> 不能自定义头）。
- 前端 `VITE_USE_MOCK`：true 走内置 mock（默认开发态），false 走真实后端。front/.env 目前设为 false。

## 本机运行
- 后端：`cd back/ai-write && mvn spring-boot:run`（8080）
- 前端：`cd front && npm run dev`（5180，/api 代理到 8080）
- 冒烟脚本：`node back/ai-write/tools/smoke-api.mjs http://127.0.0.1:8080`
- 演示账号：author@ai-write.dev / 123456，库里有一部《御兽：从一枚灰蛋开始》（18 章已确认）

## 还没做
单元测试（文档 §8 要求 JUnit5 + Testcontainers，目前只有骨架自带的 1 个空测试）、
context_snapshots 接入、P1 功能（局部重写 / 多版本比较 / 批量拆卷 / 成本报警）、
设计文档同步（目前仍写 PostgreSQL）、Docker 部署、真实大模型实测。
