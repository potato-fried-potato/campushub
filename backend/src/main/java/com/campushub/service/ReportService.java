package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Report;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ItemMapper itemMapper;

    // 提交举报（AC-11.3）
    public Map<String, Object> create(Long userId, Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long itemId = toLong(body.get("itemId"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        String detail = body.get("detail") == null ? null : String.valueOf(body.get("detail"));
        if (itemId == null || !isValidReason(reason) || (detail != null && detail.length() > 500)) {
            return error(result, 4001, "举报参数不合法");
        }
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return error(result, 4004, "商品不存在");
        }
        if (item.getSellerId().equals(userId)) {
            return error(result, 4003, "不能举报自己发布的商品");
        }
        if (reportMapper.exists(itemId, userId) > 0) {
            return error(result, 4005, "你已举报过该商品");
        }
        Report report = new Report();
        report.setItemId(itemId);
        report.setReporterId(userId);
        report.setReason(reason);
        report.setDetail(detail);
        reportMapper.insert(report);
        result.put("id", report.getId());
        result.put("status", "PENDING");
        return result;
    }

    private boolean isValidReason(String reason) {
        return "FAKE".equals(reason) || "ILLEGAL".equals(reason)
                || "SCAM".equals(reason) || "OTHER".equals(reason);
    }

    // 我提交的举报与处理结果
    public List<Report> listMine(Long userId) {
        return reportMapper.listByReporter(userId);
    }

    private Map<String, Object> error(Map<String, Object> result, int code, String message) {
        result.put("errorCode", code);
        result.put("message", message);
        return result;
    }

    private Long toLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
