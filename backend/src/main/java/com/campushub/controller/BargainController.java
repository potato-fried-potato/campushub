package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.BargainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/bargains")
public class BargainController {

    @Autowired
    private BargainService bargainService;

    @PostMapping
    public Result create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = bargainService.create(userId, body);
        if (result.containsKey("errorCode")) {
            return Result.fail((Integer) result.get("errorCode"), (String) result.get("message"));
        }
        return Result.success(result);
    }

    @GetMapping
    public Result list(@RequestParam(value = "view", required = false) String view,
                       @RequestParam(value = "status", required = false) String status,
                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(bargainService.list(userId, view, status));
    }

    @PostMapping("/{id}/accept")
    public Result accept(@PathVariable("id") Long id, HttpServletRequest request) {
        return toResult(bargainService.accept(id, (Long) request.getAttribute("userId")));
    }

    @PostMapping("/{id}/reject")
    public Result reject(@PathVariable("id") Long id, HttpServletRequest request) {
        return toResult(bargainService.reject(id, (Long) request.getAttribute("userId")));
    }

    @PostMapping("/{id}/cancel")
    public Result cancel(@PathVariable("id") Long id, HttpServletRequest request) {
        return toResult(bargainService.cancel(id, (Long) request.getAttribute("userId")));
    }

    private Result toResult(int code) {
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4003) {
            return Result.fail(4003, "没有权限操作该出价");
        }
        if (code == 4004) {
            return Result.fail(4004, "出价不存在");
        }
        return Result.fail(4005, "当前状态不允许该操作");
    }
}
