package com.campushub.service;

import com.campushub.entity.User;
import com.campushub.common.JwtUtil;
import com.campushub.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M1 认证：注册、登录与密码升级")
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User user(String storedPassword) {
        User u = new User();
        u.setId(1L);
        u.setStudentNo("542412320201");
        u.setNickname("边皓东");
        u.setRole("USER");
        u.setStatus("ACTIVE");
        u.setPassword(storedPassword);
        return u;
    }

    @Test
    @DisplayName("学号不存在时登录返回 null")
    void loginRejectsUnknownStudentNo() {
        when(userMapper.getUserByStudentNo("nobody")).thenReturn(null);

        assertThat(userService.login("nobody", "123456")).isNull();
    }

    @Test
    @DisplayName("密码错误时登录返回 null")
    void loginRejectsWrongPassword() {
        when(userMapper.getUserByStudentNo("542412320201")).thenReturn(user(encoder.encode("123456")));

        assertThat(userService.login("542412320201", "wrong-password")).isNull();
    }

    @Test
    @DisplayName("禁用账号登录返回业务码 4007")
    void loginRejectsDisabledUser() {
        User disabled = user(encoder.encode("123456"));
        disabled.setStatus("DISABLED");
        when(userMapper.getUserByStudentNo("542412320201")).thenReturn(disabled);

        Map<String, Object> result = userService.login("542412320201", "123456");

        assertThat(result).containsEntry("errorCode", 4007);
    }

    @Test
    @DisplayName("BCrypt 密码登录成功：返回三段式 token，且不回传密码")
    void loginWithBcryptPassword() {
        when(userMapper.getUserByStudentNo("542412320201")).thenReturn(user(encoder.encode("123456")));
        when(jwtUtil.create(any(), any(), any(), any())).thenReturn("header.payload.signature");

        Map<String, Object> result = userService.login("542412320201", "123456");

        assertThat(result).containsKeys("token", "user");
        assertThat(String.valueOf(result.get("token")).split("\\.")).hasSize(3);
        assertThat(((User) result.get("user")).getPassword()).isNull();
    }

    @Test
    @DisplayName("明文种子密码登录成功后自动升级为 BCrypt")
    void loginUpgradesPlaintextPassword() {
        when(userMapper.getUserByStudentNo("542412320201")).thenReturn(user("123456"));
        when(jwtUtil.create(any(), any(), any(), any())).thenReturn("header.payload.signature");

        Map<String, Object> result = userService.login("542412320201", "123456");

        assertThat(result).containsKey("token");
        verify(userMapper).updatePassword(eq(1L), argThat(p -> p.startsWith("$2")));
    }

    @Test
    @DisplayName("学号已存在时注册返回 null，且不写库")
    void registerRejectsDuplicateStudentNo() {
        when(userMapper.getUserByStudentNo("542412320201")).thenReturn(user("x"));

        Map<String, Object> result = userService.register("542412320201", "123456", "新人", "13800000000");

        assertThat(result).isNull();
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("注册成功：落库密码为 BCrypt、角色 USER、状态 ACTIVE，返回体不含密码")
    void registerCreatesActiveUser() {
        String[] storedPassword = new String[1];
        when(userMapper.getUserByStudentNo("542412320202")).thenReturn(null);
        when(jwtUtil.create(any(), any(), any(), any())).thenReturn("header.payload.signature");
        doAnswer(invocation -> {
            User toInsert = invocation.getArgument(0);
            storedPassword[0] = toInsert.getPassword();
            toInsert.setId(9L);
            return 1;
        }).when(userMapper).insertUser(any(User.class));

        Map<String, Object> result = userService.register("542412320202", "123456", "新人", "13800000000");

        assertThat(storedPassword[0]).startsWith("$2");
        assertThat(((User) result.get("user")).getPassword()).isNull();
        assertThat(((User) result.get("user")).getRole()).isEqualTo("USER");
        assertThat(((User) result.get("user")).getStatus()).isEqualTo("ACTIVE");
        assertThat(((User) result.get("user")).getAvatar()).startsWith("data:image/svg+xml;base64,");
    }

    @Test
    @DisplayName("查询用户信息时密码字段被清空")
    void getUserInfoHidesPassword() {
        when(userMapper.getUserById(1L)).thenReturn(user("$2a$hash"));

        assertThat(userService.getUserInfo(1L).getPassword()).isNull();
    }
}
