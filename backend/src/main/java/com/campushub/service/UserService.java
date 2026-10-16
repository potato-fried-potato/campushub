package com.campushub.service;

import com.campushub.entity.User;
import com.campushub.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录，成功返回 token 和用户信息，失败返回 null
     */
    public Map<String, Object> login(String studentNo, String password) {
        User user = userMapper.getUserByStudentNo(studentNo);
        if (user == null) {
            return null;
        }
        // TODO 现在密码是明文比较的，后面要改成 BCrypt（AC-1.2），先跑通流程
        if (!password.equals(user.getPassword())) {
            return null;
        }
        // 先随便生成一个 token，后面再换成 JWT
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setPassword(null); // 不要把密码返给前端
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

}
