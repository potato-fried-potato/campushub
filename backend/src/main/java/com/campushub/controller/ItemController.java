package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    // 集市商品列表（筛选 / 排序 / 分页）
    @GetMapping
    public Result list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       @RequestParam(value = "conditionLevel", required = false) String conditionLevel,
                       @RequestParam(value = "campus", required = false) String campus,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "excludeUserId", required = false) Long excludeUserId,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(itemService.getItemPage(keyword, categoryId, conditionLevel, campus,
                status, sort, excludeUserId, pageNum, pageSize));
    }

    // 我的发布（JWT 拦截还没做，先用 userId 传参）
    @GetMapping("/mine")
    public Result mine(@RequestParam("userId") Long userId,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(itemService.getMyItemsPage(userId, status, pageNum, pageSize));
    }

    // 商品详情（AC-3.1 ~ 3.5，每次访问浏览量 +1）
    @GetMapping("/{id}")
    public Result detail(@PathVariable("id") Long id) {
        Map<String, Object> data = itemService.getItemDetail(id);
        if (data == null) {
            return Result.fail(4004, "商品不存在");
        }
        return Result.success(data);
    }

    // 商品状态流转（AC-5.2，JWT 拦截还没做，先用 userId 传参）
    @PatchMapping("/{id}/status")
    public Result updateStatus(@PathVariable("id") Long id,
                               @RequestParam(value = "userId", required = false) Long userId,
                               @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!"ON_SALE".equals(status) && !"OFF_SHELF".equals(status) && !"SOLD".equals(status)) {
            return Result.fail("状态参数不合法");
        }
        int code = itemService.updateStatus(id, userId, status);
        if (code == 0) {
            return Result.success(null);
        }
        if (code == 4003) {
            return Result.fail(4003, "没有权限操作该商品");
        }
        if (code == 4004) {
            return Result.fail(4004, "商品不存在");
        }
        if (code == 4005) {
            return Result.fail(4005, "当前状态不允许这样流转");
        }
        return Result.fail("缺少用户标识");
    }

}
