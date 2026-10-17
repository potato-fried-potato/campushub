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

    // 注册
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> params) {
        String studentNo = params.get("studentNo");
        String password = params.get("password");
        String nickname = params.get("nickname");
        String phone = params.get("phone");
        if (studentNo == null || !studentNo.trim().matches("[A-Za-z0-9]{6,20}")) {
            return Result.fail("学号需为 6~20 位字母或数字");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return Result.fail("密码长度需为 6~20 位");
        }
        if (nickname == null || nickname.trim().length() == 0 || nickname.trim().length() > 16) {
            return Result.fail("昵称需为 1~16 个字");
        }
        if (phone == null || !phone.trim().matches("1\\d{10}")) {
            return Result.fail("手机号格式不正确");
        }
        Map<String, Object> data = userService.register(studentNo.trim(), password, nickname.trim(), phone.trim());
        if (data == null) {
            return Result.fail(4005, "学号已被注册");
        }
        return Result.success(data);
    }

}
