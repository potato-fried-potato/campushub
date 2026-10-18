package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Review;
import com.campushub.mapper.BargainMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BargainMapper bargainMapper;

    // 提交评价（AC-11.1、AC-11.2）
    public Map<String, Object> create(Long userId, Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long itemId = toLong(body.get("itemId"));
        Integer rating = toInt(body.get("rating"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        if (itemId == null || rating == null || rating < 1 || rating > 5
                || (content != null && content.length() > 500)) {
            return error(result, 4001, "评分需为 1~5 星，评价内容不超过 500 字");
        }
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return error(result, 4004, "商品不存在");
        }
        if (!"SOLD".equals(item.getStatus())) {
            return error(result, 4005, "商品尚未成交，暂不能评价");
        }
        Long buyerId = bargainMapper.getAcceptedBuyerId(itemId);
        Long targetId;
        if (userId.equals(item.getSellerId())) {
            targetId = buyerId;
        } else if (userId.equals(buyerId)) {
            targetId = item.getSellerId();
        } else {
            return error(result, 4003, "非交易参与方不能评价");
        }
        if (targetId == null) {
            return error(result, 4003, "非交易参与方不能评价");
        }
        if (reviewMapper.exists(itemId, userId) > 0) {
            return error(result, 4005, "该交易已评价过");
        }
        Review review = new Review();
        review.setItemId(itemId);
        review.setReviewerId(userId);
        review.setTargetId(targetId);
        review.setRating(rating);
        review.setContent(content);
        reviewMapper.insert(review);
        result.put("id", review.getId());
        return result;
    }

    // 用户信用信息与评价列表（AC-11.4）
    public Map<String, Object> listByUser(Long userId) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> summary = reviewMapper.summary(userId);
        data.put("avgRating", summary == null ? 0 : summary.get("avgRating"));
        data.put("reviewCount", summary == null ? 0 : summary.get("reviewCount"));
        data.put("list", reviewMapper.listByTarget(userId));
        return data;
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

    private Integer toInt(Object value) {
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
