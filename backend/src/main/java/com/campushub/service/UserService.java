package com.campushub.service;

import com.campushub.entity.User;
import com.campushub.common.JwtUtil;
import com.campushub.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // 直接用 BCrypt 工具类，不引入整套 Spring Security
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录，成功返回 token 和用户信息，失败返回 null
     */
    public Map<String, Object> login(String studentNo, String password) {
        User user = userMapper.getUserByStudentNo(studentNo);
        if (user == null) {
            return null;
        }
        if (!checkPassword(user, password)) {
            return null;
        }
        if ("DISABLED".equals(user.getStatus())) {
            Map<String, Object> error = new HashMap<>();
            error.put("errorCode", 4007);
            return error;
        }
        String token = JwtUtil.create(user.getId(), user.getStudentNo(), user.getNickname(), user.getRole());
        user.setPassword(null); // 不要把密码返给前端
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    /**
     * 注册，成功返回 token 和用户信息，学号重复返回 null
     */
    public Map<String, Object> register(String studentNo, String password, String nickname, String phone) {
        if (userMapper.getUserByStudentNo(studentNo) != null) {
            return null;
        }
        User user = new User();
        user.setStudentNo(studentNo);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setAvatar(buildAvatar(nickname));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userMapper.insertUser(user);

        // 注册成功直接当登录
        String token = JwtUtil.create(user.getId(), user.getStudentNo(), user.getNickname(), user.getRole());
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    /**
     * 支持 BCrypt 校验；第一天的明文种子账号登录成功后顺手升级成 BCrypt
     */
    private boolean checkPassword(User user, String password) {
        String stored = user.getPassword();
        if (stored == null) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(password, stored);
        }
        if (!password.equals(stored)) {
            return false;
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(password));
        return true;
    }

    /**
     * 注册时生成一个内联 SVG 头像（Base64 Data URI），不依赖外部图床
     */
    private String buildAvatar(String nickname) {
        String[] colors = {"#4A90D9", "#7C5CD6", "#2E9E6B", "#D68A2E", "#D64A6B"};
        int colorIndex = Math.abs(nickname.hashCode()) % colors.length;
        String firstChar = nickname.substring(0, 1);
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"128\" height=\"128\">"
                + "<rect width=\"128\" height=\"128\" rx=\"64\" fill=\"" + colors[colorIndex] + "\"/>"
                + "<text x=\"64\" y=\"84\" font-size=\"56\" text-anchor=\"middle\" fill=\"#ffffff\" font-family=\"sans-serif\">"
                + firstChar + "</text></svg>";
        String base64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    /**
     * 当前登录用户信息，密码不外传
     */
    public User getUserInfo(Long userId) {
        User user = userMapper.getUserById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

}
