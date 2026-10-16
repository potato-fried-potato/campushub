package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 登录
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String studentNo = params.get("studentNo");
        String password = params.get("password");
        if (studentNo == null || studentNo.trim().length() == 0) {
            return Result.fail("学号不能为空");
        }
        if (password == null || password.length() == 0) {
            return Result.fail("密码不能为空");
        }
        Map<String, Object> data = userService.login(studentNo.trim(), password);
        if (data == null) {
            return Result.fail(4002, "学号或密码错误");
        }
        return Result.success(data);
    }

}
