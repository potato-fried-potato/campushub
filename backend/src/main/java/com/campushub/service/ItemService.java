package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.User;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import com.campushub.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemService {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 集市列表：筛选 + 排序 + 分页（AC-2.1 ~ 2.5）
     */
    public Map<String, Object> getItemPage(String keyword, Long categoryId, String conditionLevel,
                                           String campus, String status, String sort,
                                           Long excludeUserId, Integer pageNum, Integer pageSize) {
        // 集市只展示在售和已售，其它状态一律按在售处理
        String queryStatus = "SOLD".equals(status) ? "SOLD" : "ON_SALE";
        String querySort = sort == null ? "latest" : sort;
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1 || pageSize > 50) ? 12 : pageSize;
        int offset = (page - 1) * size;

        List<Item> list = itemMapper.getItemList(keyword, categoryId, conditionLevel, campus,
                queryStatus, querySort, excludeUserId, offset, size);
        int total = itemMapper.countItems(keyword, categoryId, conditionLevel, campus,
                queryStatus, querySort, excludeUserId);
        return buildPage(list, total, page, size);
    }

    /**
     * 我的发布：状态筛选 + 分页（AC-5.1）
     */
    public Map<String, Object> getMyItemsPage(Long userId, String status, Integer pageNum, Integer pageSize) {
        String queryStatus = status;
        if (queryStatus == null || queryStatus.trim().length() == 0 || "ALL".equals(queryStatus)) {
            queryStatus = null;
        }
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1 || pageSize > 50) ? 12 : pageSize;
        int offset = (page - 1) * size;

        List<Item> list = itemMapper.getMyItems(userId, queryStatus, offset, size);
        int total = itemMapper.countMyItems(userId, queryStatus);
        return buildPage(list, total, page, size);
    }

    /**
     * 商品详情：多图 + 卖家名片，每次访问浏览量 +1（AC-3.1 ~ 3.5）
     * 商品不存在返回 null，由 Controller 转成 4004
     */
    public Map<String, Object> getItemDetail(Long itemId) {
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return null;
        }
        itemMapper.increaseViewCount(itemId);
        item.setViewCount(item.getViewCount() + 1);

        Map<String, Object> rating = itemMapper.getSellerRating(item.getSellerId());
        Map<String, Object> seller = new HashMap<>();
        seller.put("id", item.getSellerId());
        seller.put("nickname", item.getSellerName());
        seller.put("studentNo", item.getSellerStudentNo());
        seller.put("phone", item.getSellerPhone());
        seller.put("avgRating", rating == null ? 0 : rating.get("avgRating"));
        seller.put("reviewCount", rating == null ? 0 : rating.get("reviewCount"));

        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("title", item.getTitle());
        data.put("description", item.getDescription());
        data.put("price", item.getPrice());
        data.put("originalPrice", item.getOriginalPrice());
        data.put("conditionLevel", item.getConditionLevel());
        data.put("campus", item.getCampus());
        data.put("status", item.getStatus());
        data.put("reserved", item.getReserved());
        data.put("viewCount", item.getViewCount());
        data.put("categoryId", item.getCategoryId());
        data.put("categoryName", item.getCategoryName());
        data.put("images", itemMapper.getItemImages(itemId));
        data.put("seller", seller);
        data.put("createdAt", item.getCreatedAt());
        return data;
    }

    /**
     * 发布商品：参数校验 + 图片 Data URI 直存（AC-4.1 ~ 4.5）
     */
    public Map<String, Object> publish(Long userId, Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title == null || title.length() < 2 || title.length() > 40) {
            return error(4001, "标题需为 2~40 字");
        }
        Long categoryId = toLong(body.get("categoryId"));
        if (categoryId == null) {
            return error(4001, "请选择商品分类");
        }
        BigDecimal price = toDecimal(body.get("price"));
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0
                || price.compareTo(new BigDecimal("99999")) > 0) {
            return error(4001, "价格需大于 0 且不超过 99999");
        }
        String condition = str(body.get("conditionLevel"));
        if (!"NEW".equals(condition) && !"NEARLY_NEW".equals(condition) && !"GOOD".equals(condition)) {
            return error(4001, "成色参数不合法");
        }
        String campus = str(body.get("campus"));
        if (!"MAIN".equals(campus) && !"NANHU".equals(campus) && !"WEST".equals(campus)) {
            return error(4001, "校区参数不合法");
        }
        Object imagesObj = body.get("images");
        if (!(imagesObj instanceof List) || ((List<?>) imagesObj).isEmpty()) {
            return error(4001, "至少上传一张实物照片");
        }
        List<String> images = new ArrayList<>();
        for (Object o : (List<?>) imagesObj) {
            String img = String.valueOf(o);
            int index = img.indexOf(";base64,");
            if (!img.startsWith("data:image/") || index < 0) {
                return error(4006, "图片格式不合法");
            }
            try {
                byte[] raw = Base64.getDecoder().decode(img.substring(index + 8));
                if (raw.length > 5L * 1024 * 1024) {
                    return error(4006, "单张图片不能超过 5MB");
                }
            } catch (Exception e) {
                return error(4006, "图片格式不合法");
            }
            images.add(img);
        }

        Item item = new Item();
        item.setSellerId(userId);
        item.setCategoryId(categoryId);
        item.setTitle(title);
        item.setDescription(str(body.get("description")));
        item.setPrice(price);
        item.setOriginalPrice(toDecimal(body.get("originalPrice")));
        item.setConditionLevel(condition);
        item.setCampus(campus);
        item.setStatus("ON_SALE");
        item.setViewCount(0);
        itemMapper.insertItem(item);
        for (int i = 0; i < images.size(); i++) {
            itemMapper.insertImage(item.getId(), images.get(i), i);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        return result;
    }

    private Map<String, Object> buildPage(List<Item> list, int total, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", page);
        data.put("pageSize", size);
        return data;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("errorCode", code);
        result.put("message", message);
        return result;
    }

    private String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.length() == 0 ? null : text;
    }

    private Long toLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        try {
            return value == null ? null : new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 商品状态流转（AC-5.2）
     * 返回 0 表示成功，其它返回对应的业务码
     */
    public int updateStatus(Long itemId, Long userId, String targetStatus) {
        if (userId == null) {
            return 4001;
        }
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return 4004;
        }
        User operator = userMapper.getUserById(userId);
        if (operator == null) {
            return 4004;
        }
        boolean owner = item.getSellerId().equals(userId);
        boolean admin = "ADMIN".equals(operator.getRole());
        if (!owner && !admin) {
            return 4003;
        }
        if (!canChangeStatus(item.getStatus(), targetStatus)) {
            return 4005;
        }
        itemMapper.updateStatus(itemId, targetStatus);
        if ("SOLD".equals(targetStatus)) {
            // 商品成交后关闭进行中的预约（AC-10.5）
            reservationMapper.closeActiveByItem(itemId);
        }
        return 0;
    }

    // 允许：在售 -> 下架 / 已售，下架 -> 在售；已售不能再改
    private boolean canChangeStatus(String from, String to) {
        if ("ON_SALE".equals(from)) {
            return "OFF_SHELF".equals(to) || "SOLD".equals(to);
        }
        if ("OFF_SHELF".equals(from)) {
            return "ON_SALE".equals(to);
        }
        return false;
    }

}
