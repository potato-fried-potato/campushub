package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public Result create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = reportService.create((Long) request.getAttribute("userId"), body);
        if (result.containsKey("errorCode")) {
            return Result.fail((Integer) result.get("errorCode"), (String) result.get("message"));
        }
        return Result.success(result);
    }

    // 我提交的举报与处理结果
    @GetMapping("/mine")
    public Result mine(HttpServletRequest request) {
        return Result.success(reportService.listMine((Long) request.getAttribute("userId")));
    }
}
