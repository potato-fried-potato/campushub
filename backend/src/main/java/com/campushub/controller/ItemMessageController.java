package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/items/{itemId}/messages")
public class ItemMessageController {

    @Autowired
    private MessageService messageService;

    // 商品留言列表（匿名）
    @GetMapping
    public Result list(@PathVariable("itemId") Long itemId) {
        return Result.success(messageService.listByItem(itemId));
    }

    // 发表留言 / 回复（登录）
    @PostMapping
    public Result post(@PathVariable("itemId") Long itemId,
                       @RequestBody Map<String, Object> body,
                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(String.valueOf(body.get("parentId")));
        Integer error = messageService.post(userId, itemId, content, parentId);
        if (error == null) {
            return Result.success(null);
        }
        if (error == 4003) {
            return Result.fail(4003, "没有权限回复该留言");
        }
        if (error == 4004) {
            return Result.fail(4004, "留言或商品不存在");
        }
        return Result.fail(4001, "留言内容需为 1~200 字");
    }
}
