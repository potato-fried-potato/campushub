package com.campushub.controller;

import com.campushub.common.Result;
import com.campushub.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    // 平台统计（AC-8.1）
    @GetMapping("/stats")
    public Result stats() {
        return Result.success(systemService.getPlatformStats());
    }

}
