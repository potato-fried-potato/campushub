package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.mapper.ItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemService {

    @Autowired
    private ItemMapper itemMapper;

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

    public List<Item> getMyItems(Long userId) {
        return itemMapper.getMyItems(userId);
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

    private Map<String, Object> buildPage(List<Item> list, int total, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", page);
        data.put("pageSize", size);
        return data;
    }

}
