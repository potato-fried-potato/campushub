package com.campushub.service;

import com.campushub.entity.Deal;
import com.campushub.entity.Item;
import com.campushub.entity.Review;
import com.campushub.mapper.DealMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private DealMapper dealMapper;

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
        // 交易对手方由已完成的交易单确定（AC-11.1 / 11.2 / 11.6）
        Deal deal = dealMapper.getCompletedByItem(itemId);
        if (deal == null) {
            return error(result, 4003, "该商品没有已完成的交易单，不能评价");
        }
        Long targetId;
        if (userId.equals(deal.getBuyerId())) {
            targetId = deal.getSellerId();
        } else if (userId.equals(deal.getSellerId())) {
            targetId = deal.getBuyerId();
        } else {
            return error(result, 4003, "非交易双方不能评价");
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
        int delta = summary == null || summary.get("ratingDelta") == null
                ? 0 : ((Number) summary.get("ratingDelta")).intValue();
        // 信用分：基础 80 分，每条评价按星级加减（5★+10 / 4★+5 / 3★0 / 2★-5 / 1★-10），限制 0~100
        int creditScore = Math.max(0, Math.min(100, 80 + delta * 5));
        data.put("avgRating", summary == null ? 0 : summary.get("avgRating"));
        data.put("reviewCount", summary == null ? 0 : summary.get("reviewCount"));
        data.put("baseScore", 80);
        data.put("ratingDelta", delta);
        data.put("creditScore", creditScore);
        data.put("creditLevel", creditScore >= 90 ? "优秀" : creditScore >= 75 ? "良好" : creditScore >= 60 ? "一般" : "较低");
        data.put("list", reviewMapper.listByTarget(userId));
        return data;
    }

    // 我发出的评价 + 待我评价的成交商品
    public Map<String, Object> listMine(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", reviewMapper.listByReviewer(userId));
        List<Map<String, Object>> pending = new ArrayList<>();
        for (Deal deal : dealMapper.listPendingReview(userId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("dealId", deal.getId());
            row.put("itemId", deal.getItemId());
            row.put("itemTitle", deal.getItemTitle());
            row.put("dealPrice", deal.getDealPrice());
            row.put("completedAt", deal.getCompletedAt());
            row.put("targetNickname", userId.equals(deal.getBuyerId())
                    ? deal.getSellerNickname() : deal.getBuyerNickname());
            pending.add(row);
        }
        data.put("pending", pending);
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
