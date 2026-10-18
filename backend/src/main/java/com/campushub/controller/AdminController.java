package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public Result users(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false) String status,
                        @RequestParam(value = "pageNum", required = false) Integer pageNum,
                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
                        HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        return Result.success(adminService.users(keyword, status, pageNum, pageSize));
    }

    @PatchMapping("/users/{id}/status")
    public Result updateUserStatus(@PathVariable("id") Long id,
                                   @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        int code = adminService.updateUserStatus(id, body.get("status"));
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4004) {
            return Result.fail(4004, "用户不存在");
        }
        if (code == 4005) {
            return Result.fail(4005, "不能禁用管理员账号");
        }
        return Result.fail(4001, "状态参数不合法");
    }

    @GetMapping("/items")
    public Result items(@RequestParam(value = "status", required = false) String status,
                        @RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "pageNum", required = false) Integer pageNum,
                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
                        HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        return Result.success(adminService.items(status, keyword, pageNum, pageSize));
    }

    @PatchMapping("/items/{id}/off-shelf")
    public Result offShelf(@PathVariable("id") Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        int code = adminService.offShelf(id);
        if (code == 0) {
            return Result.success(null);
        }
        return Result.fail(4004, "商品不存在");
    }

    @GetMapping("/reports")
    public Result reports(@RequestParam(value = "status", required = false) String status,
                          @RequestParam(value = "pageNum", required = false) Integer pageNum,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                          HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        return Result.success(adminService.reports(status, pageNum, pageSize));
    }

    @PatchMapping("/reports/{id}")
    public Result handleReport(@PathVariable("id") Long id,
                               @RequestBody Map<String, String> body,
                               HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.fail(4003, "仅管理员可访问");
        }
        int code = adminService.handleReport(id, body.get("action"), (Long) request.getAttribute("userId"));
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4004) {
            return Result.fail(4004, "举报不存在");
        }
        return Result.fail(4001, "处置动作不合法");
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(request.getAttribute("role"));
    }
}
