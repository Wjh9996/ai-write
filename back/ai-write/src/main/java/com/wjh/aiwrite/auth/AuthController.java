package com.wjh.aiwrite.auth;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.common.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 账号模块 —— POST /api/auth/login、/logout、GET /api/auth/me
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Data
    public static class LoginRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少 6 位")
        private String password;
    }

    /** 返回体与前端 UserProfile 对齐 */
    @Data
    @AllArgsConstructor
    public static class UserProfile {
        private Long id;
        private String email;
        private String nickname;
        private String avatarColor;
        private String plan;
        private Integer monthlyQuotaChars;
        private Integer usedChars;
        private String token;
    }

    @PostMapping("/login")
    public ApiResult<UserProfile> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().trim())
                .orElseThrow(() -> BizException.badRequest("邮箱或密码不正确"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BizException.badRequest("邮箱或密码不正确");
        }

        String token = tokenService.issue(user);
        log.info("[auth] login userId={}", user.getId());
        return ApiResult.ok(toProfile(user, token), "登录成功");
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        tokenService.revoke(bearer(auth));
        return ApiResult.ok(null, "已退出");
    }

    @GetMapping("/me")
    public ApiResult<UserProfile> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = tokenService.resolve(bearer(auth))
                .orElseThrow(() -> BizException.forbidden("未登录"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.forbidden("登录已失效，请重新登录"));
        return ApiResult.ok(toProfile(user, bearer(auth)));
    }

    /** 便于本地开发：首次启动时若账号不存在，可用此接口自助建号 */
    @PostMapping("/register")
    public ApiResult<UserProfile> register(@Valid @RequestBody LoginRequest req) {
        String email = req.getEmail().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw BizException.badRequest("该邮箱已注册");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(email.substring(0, Math.min(email.indexOf('@') > 0 ? email.indexOf('@') : 2, 8)));
        user.setPlan("PRO");
        User saved = userRepository.save(user);
        String token = tokenService.issue(saved);
        return ApiResult.ok(toProfile(saved, token), "注册成功");
    }

    private static String bearer(String authorization) {
        if (authorization == null) return null;
        String v = authorization.trim();
        return v.regionMatches(true, 0, "Bearer ", 0, 7) ? v.substring(7).trim() : v;
    }

    private static UserProfile toProfile(User u, String token) {
        return new UserProfile(
                u.getId(),
                u.getEmail(),
                u.getNickname(),
                "linear-gradient(135deg,#6366f1 0%,#ec4899 100%)",
                u.getPlan(),
                u.getMonthlyQuotaChars(),
                u.getUsedChars(),
                token
        );
    }
}
