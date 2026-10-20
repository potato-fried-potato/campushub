package com.campushub.service;

import com.campushub.entity.Bargain;
import com.campushub.entity.Item;
import com.campushub.mapper.BargainMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M3 砍价：8 折下限与状态校验")
class BargainServiceTest {

    @Mock private BargainMapper bargainMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ReservationMapper reservationMapper;
    @Mock private DealService dealService;
    @InjectMocks private BargainService bargainService;

    private Item onSaleItem;

    @BeforeEach
    void setUp() {
        onSaleItem = new Item();
        onSaleItem.setId(10L);
        onSaleItem.setSellerId(1L);
        onSaleItem.setPrice(new BigDecimal("25.00"));
        onSaleItem.setStatus("ON_SALE");
    }

    private Map<String, Object> body(String offer) {
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", 10L);
        body.put("offerPrice", offer);
        body.put("message", "测试出价");
        return body;
    }

    @Test
    @DisplayName("砍价下限 = 挂牌价 × 0.8，四舍五入两位小数")
    void floorPriceRoundsToTwoDecimals() {
        assertThat(BargainService.floorPrice(new BigDecimal("25.00")))
                .isEqualByComparingTo("20.00");
        assertThat(BargainService.floorPrice(new BigDecimal("39.90")))
                .isEqualByComparingTo("31.92");
        assertThat(BargainService.floorPrice(new BigDecimal("12.34")))
                .isEqualByComparingTo("9.87");
    }

    @Test
    @DisplayName("恰好 8 折可以出价")
    void createAcceptsExactFloor() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);
        when(bargainMapper.getPendingByItemAndBuyer(10L, 2L)).thenReturn(null);

        Map<String, Object> result = bargainService.create(2L, body("20.00"));

        assertThat(result).doesNotContainKey("errorCode");
        assertThat(result.get("status")).isEqualTo("PENDING");
        verify(bargainMapper).insert(any(Bargain.class));
    }

    @Test
    @DisplayName("低于 8 折被拒绝（4001）")
    void createRejectsBelowFloor() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);

        Map<String, Object> result = bargainService.create(2L, body("19.99"));

        assertThat(result.get("errorCode")).isEqualTo(4001);
        assertThat(String.valueOf(result.get("message"))).contains("8 折");
        verify(bargainMapper, never()).insert(any(Bargain.class));
    }

    @Test
    @DisplayName("高于或等于挂牌价被拒绝（4001）")
    void createRejectsOfferAtOrAbovePrice() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);

        Map<String, Object> result = bargainService.create(2L, body("25.00"));

        assertThat(result.get("errorCode")).isEqualTo(4001);
        assertThat(String.valueOf(result.get("message"))).contains("低于挂牌价");
    }

    @Test
    @DisplayName("不能对自己的商品出价（4003）")
    void createRejectsSelfBargain() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);

        Map<String, Object> result = bargainService.create(1L, body("20.00"));

        assertThat(result.get("errorCode")).isEqualTo(4003);
    }

    @Test
    @DisplayName("商品存在待成交交易单时不可出价（4005）")
    void createRejectsWhenDealActive() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);
        when(dealService.hasActiveDeal(10L)).thenReturn(true);

        Map<String, Object> result = bargainService.create(2L, body("20.00"));

        assertThat(result.get("errorCode")).isEqualTo(4005);
    }

    @Test
    @DisplayName("同一买家重复待处理出价被拒绝（4005）")
    void createRejectsDuplicatePending() {
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);
        when(bargainMapper.getPendingByItemAndBuyer(10L, 2L)).thenReturn(99L);

        Map<String, Object> result = bargainService.create(2L, body("20.00"));

        assertThat(result.get("errorCode")).isEqualTo(4005);
    }

    @Test
    @DisplayName("卖家接受出价后生成交易单（AC-6.4）")
    void acceptCreatesDeal() {
        Bargain bargain = new Bargain();
        bargain.setId(7L);
        bargain.setItemId(10L);
        bargain.setBuyerId(2L);
        bargain.setSellerId(1L);
        bargain.setOfferPrice(new BigDecimal("20.00"));
        bargain.setStatus("PENDING");
        when(bargainMapper.getById(7L)).thenReturn(bargain);
        when(itemMapper.getItemById(10L)).thenReturn(onSaleItem);
        when(dealService.createFromBargain(onSaleItem, 2L, new BigDecimal("20.00"), 7L)).thenReturn(0);

        int code = bargainService.accept(7L, 1L);

        assertThat(code).isZero();
        verify(bargainMapper).updateStatus(7L, "ACCEPTED");
        verify(dealService).createFromBargain(eq(onSaleItem), eq(2L), eq(new BigDecimal("20.00")), eq(7L));
    }

    @Test
    @DisplayName("非卖家不能接受出价（4003）")
    void acceptRejectsNonSeller() {
        Bargain bargain = new Bargain();
        bargain.setSellerId(1L);
        bargain.setStatus("PENDING");
        when(bargainMapper.getById(7L)).thenReturn(bargain);

        assertThat(bargainService.accept(7L, 9L)).isEqualTo(4003);
        verify(dealService, never()).createFromBargain(any(), any(), any(), any());
    }
}
