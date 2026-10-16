package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    // 首页商品列表
    @GetMapping
    public Result list() {
        return Result.success(itemService.getItemList());
    }

    // 我的发布（第一天先直接用 userId 传参，登录拦截后面再做）
    @GetMapping("/mine")
    public Result mine(@RequestParam("userId") Long userId) {
        return Result.success(itemService.getMyItems(userId));
    }

}
