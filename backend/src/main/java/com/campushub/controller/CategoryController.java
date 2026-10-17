package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 分类列表（带在售数量，AC-2.2）
    @GetMapping("/categories")
    public Result list() {
        return Result.success(categoryService.getCategoryList());
    }

}
