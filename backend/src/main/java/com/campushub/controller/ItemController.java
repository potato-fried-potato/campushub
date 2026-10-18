package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       HttpServletRequest request) {
        // 登录态访问时自动排除自己发布的商品（AC-2.5）
        Long excludeUserId = (Long) request.getAttribute("userId");
        return Result.success(itemService.getItemPage(keyword, categoryId, conditionLevel, campus,
                status, sort, excludeUserId, pageNum, pageSize));
    }

    // 我的发布（AC-5.1）
    @GetMapping("/mine")
    public Result mine(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(itemService.getMyItemsPage(userId, status, pageNum, pageSize));
    }

    // 商品详情（AC-3.1 ~ 3.5，每次访问浏览量 +1）
    @GetMapping("/{id}")
    public Result detail(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> data = itemService.getItemDetail(id, userId);
        if (data == null) {
            return Result.fail(4004, "商品不存在");
        }
        return Result.success(data);
    }

    // 发布商品（AC-4.1 ~ 4.5）
    @PostMapping
    public Result publish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = itemService.publish(userId, body);
        if (result.containsKey("errorCode")) {
            return Result.fail((Integer) result.get("errorCode"), (String) result.get("message"));
        }
        return Result.success(result);
    }

    // 商品状态流转（AC-5.2）
    @PatchMapping("/{id}/status")
    public Result updateStatus(@PathVariable("id") Long id,
                               @RequestBody Map<String, String> body,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
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
