package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
