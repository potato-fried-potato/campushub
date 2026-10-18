package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 我的留言（收到 / 发出）
    @GetMapping("/mine")
    public Result mine(@RequestParam(value = "view", required = false) String view,
                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.listMine(userId, view));
    }

    // 标记已读
    @PatchMapping("/{id}/read")
    public Result read(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!messageService.markRead(id, userId)) {
            return Result.fail(4003, "没有权限操作该留言");
        }
        return Result.success(null);
    }
}
