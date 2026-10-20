package com.campushub.service;

import com.campushub.entity.Deal;
import com.campushub.entity.Item;
import com.campushub.entity.User;
import com.campushub.mapper.DealMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M2 商品详情与状态：隐私、砍价下限与锁定")
class ItemServiceTest {

    @Mock private ItemMapper itemMapper;
    @Mock private UserMapper userMapper;
    @Mock private ReservationMapper reservationMapper;
    @Mock private DealMapper dealMapper;
    @Mock private ReviewMapper reviewMapper;
    @InjectMocks private ItemService itemService;

    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(10L);
        item.setSellerId(1L);
        item.setTitle("测试商品");
        item.setPrice(new BigDecimal("25.00"));
        item.setStatus("ON_SALE");
        item.setViewCount(5);
        item.setReserved(false);
        item.setSellerName("卖家");
        item.setSellerStudentNo("542412320201");
        item.setCategoryName("教材图书");
    }

    @Test
    @DisplayName("详情不返回卖家手机号，并返回砍价下限")
    void detailDoesNotExposePhone() {
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(itemMapper.getSellerRating(1L)).thenReturn(Map.of(
                "avgRating", 4.5, "reviewCount", 2, "ratingDelta", 2));
        when(itemMapper.getItemImages(10L)).thenReturn(List.of("data:image/png;base64,x"));
        when(dealMapper.getActiveByItem(10L)).thenReturn(null);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(null);

        Map<String, Object> data = itemService.getItemDetail(10L, 2L);

        @SuppressWarnings("unchecked")
        Map<String, Object> seller = (Map<String, Object>) data.get("seller");
        assertThat(seller).doesNotContainKey("phone");
        assertThat(data.get("bargainFloor")).isEqualTo(new BigDecimal("20.00"));
        assertThat(data.get("canDirectBuy")).isEqualTo(true);
        assertThat(data.get("viewCount")).isEqualTo(6);
    }

    @Test
    @DisplayName("存在待成交交易单时详情标记 reserved 且不可直接购买")
    void detailReflectsPendingDeal() {
        item.setReserved(true);
        Deal active = new Deal();
        active.setId(9L);
        active.setItemId(10L);
        active.setBuyerId(2L);
        active.setSellerId(1L);
        active.setStatus("PENDING");
        active.setSource("DIRECT");
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(itemMapper.getSellerRating(1L)).thenReturn(Map.of(
                "avgRating", 4.5, "reviewCount", 2, "ratingDelta", 2));
        when(itemMapper.getItemImages(10L)).thenReturn(List.of());
        when(dealMapper.getActiveByItem(10L)).thenReturn(active);

        Map<String, Object> data = itemService.getItemDetail(10L, 2L);

        assertThat(data.get("dealStatus")).isEqualTo("PENDING");
        assertThat(data.get("canDirectBuy")).isEqualTo(false);
        assertThat(data.get("isBuyer")).isEqualTo(true);
    }

    @Test
    @DisplayName("已完成且已评价后不再显示评价入口")
    void completedAndReviewedHidesReviewEntry() {
        item.setStatus("SOLD");
        Deal completed = new Deal();
        completed.setId(9L);
        completed.setItemId(10L);
        completed.setBuyerId(2L);
        completed.setSellerId(1L);
        completed.setStatus("COMPLETED");
        completed.setSource("DIRECT");
        completed.setDealPrice(new BigDecimal("25.00"));
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(itemMapper.getSellerRating(1L)).thenReturn(Map.of(
                "avgRating", 4.5, "reviewCount", 2, "ratingDelta", 2));
        when(itemMapper.getItemImages(10L)).thenReturn(List.of());
        when(dealMapper.getActiveByItem(10L)).thenReturn(null);
        when(dealMapper.getCompletedByItem(10L)).thenReturn(completed);
        when(reviewMapper.exists(10L, 2L)).thenReturn(1);

        Map<String, Object> data = itemService.getItemDetail(10L, 2L);

        assertThat(data.get("dealStatus")).isEqualTo("COMPLETED");
        assertThat(data.get("canReview")).isEqualTo(false);
    }

    @Test
    @DisplayName("卖家标记线下已售：写快照方法为 markOfflineSold 并关闭预约")
    void offlineSoldClosesReservations() {
        User owner = new User();
        owner.setId(1L);
        owner.setRole("USER");
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(userMapper.getUserById(1L)).thenReturn(owner);
        when(dealMapper.getActiveByItem(10L)).thenReturn(null);

        int code = itemService.updateStatus(10L, 1L, "SOLD");

        assertThat(code).isZero();
        verify(itemMapper).markOfflineSold(10L);
        verify(reservationMapper).closeActiveByItem(10L);
    }

    @Test
    @DisplayName("存在待成交交易单时卖家不能手动改状态（4005）")
    void sellerCannotChangeStatusWithActiveDeal() {
        User owner = new User();
        owner.setId(1L);
        owner.setRole("USER");
        Deal active = new Deal();
        active.setId(9L);
        active.setStatus("PENDING");
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(userMapper.getUserById(1L)).thenReturn(owner);
        when(dealMapper.getActiveByItem(10L)).thenReturn(active);

        int code = itemService.updateStatus(10L, 1L, "OFF_SHELF");

        assertThat(code).isEqualTo(4005);
        verify(itemMapper, never()).updateStatus(10L, "OFF_SHELF");
        verify(itemMapper, never()).markOfflineSold(10L);
    }
}
