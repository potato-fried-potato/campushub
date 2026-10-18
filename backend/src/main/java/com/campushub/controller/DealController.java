package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.DealService;
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
@RequestMapping("/api/deals")
public class DealController {

    @Autowired
    private DealService dealService;

    // 立即购买：按挂牌价生成待成交交易单
    @PostMapping("/direct")
    public Result direct(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = dealService.direct(userId, body);
        if (result.containsKey("errorCode")) {
            return Result.fail((Integer) result.get("errorCode"), (String) result.get("message"));
        }
        return Result.success(result);
    }

    // 我的交易单（双视角）
    @GetMapping
    public Result list(@RequestParam(value = "view", required = false) String view,
                       @RequestParam(value = "status", required = false) String status,
                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(dealService.list(userId, view, status));
    }

    @PostMapping("/{id}/complete")
    public Result complete(@PathVariable("id") Long id, HttpServletRequest request) {
        return toResult(dealService.complete(id, (Long) request.getAttribute("userId")));
    }

    @PostMapping("/{id}/cancel")
    public Result cancel(@PathVariable("id") Long id, HttpServletRequest request) {
        return toResult(dealService.cancel(id, (Long) request.getAttribute("userId")));
    }

    // 交易单私密会话（仅买卖双方可见）
    @GetMapping("/{id}/messages")
    public Result messages(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> data = dealService.messages(id, userId);
        if (data.containsKey("errorCode")) {
            return Result.fail((Integer) data.get("errorCode"), (String) data.get("message"));
        }
        return Result.success(data);
    }

    @PostMapping("/{id}/messages")
    public Result sendMessage(@PathVariable("id") Long id,
                              @RequestBody Map<String, Object> body,
                              HttpServletRequest request) {
        int code = dealService.sendMessage(id, (Long) request.getAttribute("userId"), body);
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4001) {
            return Result.fail(4001, "消息内容需为 1~200 字");
        }
        return toResult(code);
    }

    private Result toResult(int code) {
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4003) {
            return Result.fail(4003, "没有权限操作该交易单");
        }
        if (code == 4004) {
            return Result.fail(4004, "交易单不存在");
        }
        return Result.fail(4005, "当前状态不允许该操作");
    }
}
