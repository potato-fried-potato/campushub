package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ReviewService;
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
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/reviews")
    public Result create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = reviewService.create((Long) request.getAttribute("userId"), body);
        if (result.containsKey("errorCode")) {
            return Result.fail((Integer) result.get("errorCode"), (String) result.get("message"));
        }
        return Result.success(result);
    }

    // 用户评价列表（匿名）
    @GetMapping("/users/{userId}/reviews")
    public Result list(@PathVariable("userId") Long userId) {
        return Result.success(reviewService.listByUser(userId));
    }
}
