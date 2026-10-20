package com.campushub.service;

import com.campushub.entity.Deal;
import com.campushub.entity.Item;
import com.campushub.entity.Review;
import com.campushub.mapper.DealMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M5 评价：资格、去重与信用分")
class ReviewServiceTest {

    @Mock private ReviewMapper reviewMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private DealMapper dealMapper;
    @InjectMocks private ReviewService reviewService;

    private Item soldItem;
    private Deal completedDeal;

    @BeforeEach
    void setUp() {
        soldItem = new Item();
        soldItem.setId(10L);
        soldItem.setSellerId(1L);
        soldItem.setStatus("SOLD");

        completedDeal = new Deal();
        completedDeal.setId(9L);
        completedDeal.setItemId(10L);
        completedDeal.setBuyerId(2L);
        completedDeal.setSellerId(1L);
        completedDeal.setStatus("COMPLETED");
    }

    private Map<String, Object> body(int rating) {
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", 10L);
        body.put("rating", rating);
        body.put("content", "测试评价");
        return body;
    }

    @Test
    @DisplayName("买家评价卖家：target 为卖家")
    void buyerReviewsSeller() {
        when(itemMapper.getItemById(10L)).thenReturn(soldItem);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(completedDeal);
        when(reviewMapper.exists(10L, 2L)).thenReturn(0);

        Map<String, Object> result = reviewService.create(2L, body(5));

        assertThat(result).doesNotContainKey("errorCode");
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getTargetId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("非交易双方不能评价（4003）")
    void outsiderCannotReview() {
        when(itemMapper.getItemById(10L)).thenReturn(soldItem);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(completedDeal);

        Map<String, Object> result = reviewService.create(3L, body(5));

        assertThat(result.get("errorCode")).isEqualTo(4003);
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    @DisplayName("重复评价被拒绝（4005）")
    void duplicateReviewRejected() {
        when(itemMapper.getItemById(10L)).thenReturn(soldItem);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(completedDeal);
        when(reviewMapper.exists(10L, 2L)).thenReturn(1);

        assertThat(reviewService.create(2L, body(5)).get("errorCode")).isEqualTo(4005);
    }

    @Test
    @DisplayName("未成交商品不能评价（4005）")
    void unsoldItemCannotReview() {
        soldItem.setStatus("ON_SALE");
        when(itemMapper.getItemById(10L)).thenReturn(soldItem);

        assertThat(reviewService.create(2L, body(5)).get("errorCode")).isEqualTo(4005);
    }

    @Test
    @DisplayName("线下已售（无交易单）不能评价（4003）")
    void offlineSoldCannotReview() {
        when(itemMapper.getItemById(10L)).thenReturn(soldItem);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(null);

        assertThat(reviewService.create(2L, body(5)).get("errorCode")).isEqualTo(4003);
    }

    @Test
    @DisplayName("评分越界返回 4001")
    void invalidRatingRejected() {
        assertThat(reviewService.create(2L, body(6)).get("errorCode")).isEqualTo(4001);
    }

    @Test
    @DisplayName("信用分 = 基础 80 + 星级差额 × 5")
    void creditScoreCalculation() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("avgRating", 4.5);
        summary.put("reviewCount", 2);
        summary.put("ratingDelta", 3);
        when(reviewMapper.summary(1L)).thenReturn(summary);
        when(reviewMapper.listByTarget(1L)).thenReturn(List.of());

        Map<String, Object> data = reviewService.listByUser(1L);

        assertThat(data.get("baseScore")).isEqualTo(80);
        assertThat(data.get("creditScore")).isEqualTo(95);
        assertThat(data.get("creditLevel")).isEqualTo("优秀");
    }
}
