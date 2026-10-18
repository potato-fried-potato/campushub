package com.campushub.controller;

import com.campushub.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    // 图片上传，校验文件头魔数，转 Data URI 直存（AC-4.2、4.3）
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.getSize() == 0) {
            return Result.fail(4006, "请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail(4006, "图片不能超过 5MB");
        }
        byte[] bytes = file.getBytes();
        String type = detectType(bytes);
        if (type == null) {
            return Result.fail(4006, "仅支持 JPG / PNG / WEBP / GIF 图片");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("url", "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes));
        return Result.success(data);
    }

    private String detectType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
