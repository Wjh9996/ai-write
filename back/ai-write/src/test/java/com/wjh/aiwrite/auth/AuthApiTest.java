package com.wjh.aiwrite.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 接口层测试：登录、鉴权、统一响应结构
 *
 * 覆盖规则⑧：所有写入按 user_id + novel_id 做权限校验。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NovelRepository novelRepository;
    @Autowired
    private TestFixtures fixtures;

    /** 注册一个账号并登录，返回 token */
    private String registerAndLogin() throws Exception {
        String email = "u_" + System.nanoTime() + "@ai-write.dev";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        return login(email, "test1234");
    }

    private String login(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(res.getResponse().getContentAsString()).get("data");
        return data.get("token").asText();
    }

    @Test
    @DisplayName("注册 → 登录 → 拿到 token，响应是统一结构")
    void registerThenLoginThenMe() throws Exception {
        String email = "api_" + System.nanoTime() + "@ai-write.dev";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                // 统一响应必须带 requestId
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        String token = login(email, "test1234");
        assertFalse(token.isBlank());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("密码错误：400 且是统一错误结构")
    void wrongPassword() throws Exception {
        String email = "bad_" + System.nanoTime() + "@ai-write.dev";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("未带 token：403，接口不能裸奔")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/novels"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("规则⑧：不能访问别人的作品")
    void cannotAccessOthersNovel() throws Exception {
        var mine = fixtures.full();
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        // mine 的作品属于 user_id=1，两个测试账号都不是 1 → 都应被拒
        mockMvc.perform(get("/api/novels/" + mine.novel().getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/novels/" + mine.novel().getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("参数校验：作品名为空 → 400")
    void validation() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/novels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
