package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Report;
import com.campushub.entity.User;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ReportMapper reportMapper;

    // 用户管理（AC-12.2）
    public Map<String, Object> users(String keyword, String status, Integer pageNum, Integer pageSize) {
        int page = page(pageNum);
        int size = size(pageSize);
        List<User> list = userMapper.listForAdmin(keyword, status, (page - 1) * size, size);
        list.forEach(u -> u.setPassword(null));
        return page(list, userMapper.countForAdmin(keyword, status), page, size);
    }

    public int updateUserStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            return 4001;
        }
        User user = userMapper.getUserById(id);
        if (user == null) {
            return 4004;
        }
        if ("ADMIN".equals(user.getRole())) {
            return 4005;
        }
        userMapper.updateStatus(id, status);
        return 0;
    }

    // 商品管理（AC-12.3）
    public Map<String, Object> items(String status, String keyword, Integer pageNum, Integer pageSize) {
        int page = page(pageNum);
        int size = size(pageSize);
        List<Item> list = itemMapper.adminList(status, keyword, (page - 1) * size, size);
        return page(list, itemMapper.adminCount(status, keyword), page, size);
    }

    public int offShelf(Long id) {
        Item item = itemMapper.getItemById(id);
        if (item == null) {
            return 4004;
        }
        itemMapper.updateStatus(id, "OFF_SHELF");
        return 0;
    }

    // 举报处置（AC-12.4）
    public Map<String, Object> reports(String status, Integer pageNum, Integer pageSize) {
        int page = page(pageNum);
        int size = size(pageSize);
        List<Report> list = reportMapper.list(status, (page - 1) * size, size);
        return page(list, reportMapper.count(status), page, size);
    }

    public int handleReport(Long id, String action, Long adminId) {
        Report report = reportMapper.getById(id);
        if (report == null) {
            return 4004;
        }
        if ("OFF_SHELF".equals(action)) {
            reportMapper.updateStatus(id, "OFF_SHELF", adminId);
            itemMapper.updateStatus(report.getItemId(), "OFF_SHELF");
            return 0;
        }
        if ("REJECT".equals(action)) {
            reportMapper.updateStatus(id, "REJECTED", adminId);
            return 0;
        }
        return 4001;
    }

    private int page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int size(Integer pageSize) {
        return pageSize == null || pageSize < 1 || pageSize > 100 ? 10 : pageSize;
    }

    private Map<String, Object> page(List<?> list, int total, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", page);
        data.put("pageSize", size);
        return data;
    }
}
