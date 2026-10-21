package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Reservation;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M3 预约：提交、确认、拒绝与取消")
class ReservationServiceTest {

    private static final Long ITEM_ID = 10L;
    private static final Long SELLER_ID = 1L;
    private static final Long BUYER_ID = 2L;

    @Mock private ReservationMapper reservationMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private DealService dealService;
    @InjectMocks private ReservationService reservationService;

    private Item item(String status) {
        Item item = new Item();
        item.setId(ITEM_ID);
        item.setSellerId(SELLER_ID);
        item.setPrice(new BigDecimal("120.00"));
        item.setStatus(status);
        return item;
    }

    private Reservation reservation(String status) {
        Reservation r = new Reservation();
        r.setId(5L);
        r.setItemId(ITEM_ID);
        r.setBuyerId(BUYER_ID);
        r.setSellerId(SELLER_ID);
        r.setStatus(status);
        return r;
    }

    private Map<String, Object> body() {
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", ITEM_ID);
        body.put("remark", "东风校区图书馆门口");
        return body;
    }

    @Test
    @DisplayName("缺少商品 id 返回 4001")
    void createRequiresItemId() {
        assertThat(reservationService.create(BUYER_ID, new HashMap<>())).containsEntry("errorCode", 4001);
    }

    @Test
    @DisplayName("商品不存在返回 4004")
    void createRejectsUnknownItem() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(null);

        assertThat(reservationService.create(BUYER_ID, body())).containsEntry("errorCode", 4004);
    }

    @Test
    @DisplayName("不能预约自己发布的商品（4003）")
    void createRejectsSelfReservation() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(item("ON_SALE"));

        assertThat(reservationService.create(SELLER_ID, body())).containsEntry("errorCode", 4003);
    }

    @Test
    @DisplayName("商品非在售时不可预约（4005）")
    void createRejectsNonOnSaleItem() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(item("SOLD"));

        assertThat(reservationService.create(BUYER_ID, body())).containsEntry("errorCode", 4005);
    }

    @Test
    @DisplayName("商品已有待成交交易单时不可预约（4005）")
    void createRejectsWhenDealActive() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(item("ON_SALE"));
        when(dealService.hasActiveDeal(ITEM_ID)).thenReturn(true);

        assertThat(reservationService.create(BUYER_ID, body())).containsEntry("errorCode", 4005);
    }

    @Test
    @DisplayName("同一买家重复预约被拒绝（4005）")
    void createRejectsDuplicateReservation() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(item("ON_SALE"));
        when(dealService.hasActiveDeal(ITEM_ID)).thenReturn(false);
        when(reservationMapper.hasActive(ITEM_ID, BUYER_ID)).thenReturn(1);

        assertThat(reservationService.create(BUYER_ID, body())).containsEntry("errorCode", 4005);
    }

    @Test
    @DisplayName("预约提交成功：写入 PENDING 记录")
    void createInsertsPendingReservation() {
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(item("ON_SALE"));
        when(dealService.hasActiveDeal(ITEM_ID)).thenReturn(false);
        when(reservationMapper.hasActive(ITEM_ID, BUYER_ID)).thenReturn(0);

        Map<String, Object> result = reservationService.create(BUYER_ID, body());

        assertThat(result).containsEntry("status", "PENDING");
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationMapper).insert(captor.capture());
        assertThat(captor.getValue().getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(captor.getValue().getRemark()).isEqualTo("东风校区图书馆门口");
    }

    @Test
    @DisplayName("非卖家不能确认预约（4003）")
    void confirmRejectsNonSeller() {
        when(reservationMapper.getById(5L)).thenReturn(reservation("PENDING"));

        assertThat(reservationService.confirm(5L, BUYER_ID)).isEqualTo(4003);
    }

    @Test
    @DisplayName("非 PENDING 状态不能确认（4005）")
    void confirmRejectsNonPending() {
        when(reservationMapper.getById(5L)).thenReturn(reservation("CANCELLED"));

        assertThat(reservationService.confirm(5L, SELLER_ID)).isEqualTo(4005);
    }

    @Test
    @DisplayName("确认预约：状态置为 CONFIRMED 并按挂牌价生成交易单")
    void confirmCreatesDealAtListPrice() {
        Item onSale = item("ON_SALE");
        when(reservationMapper.getById(5L)).thenReturn(reservation("PENDING"));
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(onSale);
        when(dealService.hasActiveDeal(ITEM_ID)).thenReturn(false);
        when(dealService.createFromReservation(onSale, BUYER_ID, new BigDecimal("120.00"), 5L)).thenReturn(0);

        assertThat(reservationService.confirm(5L, SELLER_ID)).isEqualTo(0);
        verify(reservationMapper).updateStatus(5L, "CONFIRMED");
    }

    @Test
    @DisplayName("生成交易单失败时抛出异常以回滚状态更新")
    void confirmRollsBackWhenDealFails() {
        Item onSale = item("ON_SALE");
        when(reservationMapper.getById(5L)).thenReturn(reservation("PENDING"));
        when(itemMapper.getItemById(ITEM_ID)).thenReturn(onSale);
        when(dealService.hasActiveDeal(ITEM_ID)).thenReturn(false);
        when(dealService.createFromReservation(eq(onSale), eq(BUYER_ID), any(), eq(5L))).thenReturn(4005);

        assertThatThrownBy(() -> reservationService.confirm(5L, SELLER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("卖家拒绝预约：状态置为 REJECTED")
    void rejectMarksRejected() {
        when(reservationMapper.getById(5L)).thenReturn(reservation("PENDING"));

        assertThat(reservationService.reject(5L, SELLER_ID)).isEqualTo(0);
        verify(reservationMapper).updateStatus(5L, "REJECTED");
    }

    @Test
    @DisplayName("非买家不能取消预约（4003）")
    void cancelRejectsNonBuyer() {
        when(reservationMapper.getById(5L)).thenReturn(reservation("PENDING"));

        assertThat(reservationService.cancel(5L, SELLER_ID)).isEqualTo(4003);
        verify(reservationMapper, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("已确认的预约买家仍可取消")
    void cancelAllowsConfirmed() {
        when(reservationMapper.getById(5L)).thenReturn(reservation("CONFIRMED"));

        assertThat(reservationService.cancel(5L, BUYER_ID)).isEqualTo(0);
        verify(reservationMapper).updateStatus(5L, "CANCELLED");
    }
}
