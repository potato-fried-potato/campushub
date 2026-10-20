package com.campushub.service;

import com.campushub.entity.Deal;
import com.campushub.entity.DealMessage;
import com.campushub.entity.Item;
import com.campushub.mapper.BargainMapper;
import com.campushub.mapper.DealMapper;
import com.campushub.mapper.DealMessageMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M3 交易单：三路径、状态机与私聊权限")
class DealServiceTest {

    @Mock private DealMapper dealMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private BargainMapper bargainMapper;
    @Mock private ReservationMapper reservationMapper;
    @Mock private DealMessageMapper dealMessageMapper;
    @InjectMocks private DealService dealService;

    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(10L);
        item.setSellerId(1L);
        item.setTitle("测试商品");
        item.setPrice(new BigDecimal("25.00"));
        item.setStatus("ON_SALE");
    }

    private Deal deal(Long id, String status, String source, Long sourceId) {
        Deal deal = new Deal();
        deal.setId(id);
        deal.setItemId(10L);
        deal.setBuyerId(2L);
        deal.setSellerId(1L);
        deal.setDealPrice(new BigDecimal("25.00"));
        deal.setStatus(status);
        deal.setSource(source);
        deal.setSourceId(sourceId);
        deal.setItemTitle("测试商品");
        deal.setBuyerNickname("买家");
        deal.setSellerNickname("卖家");
        return deal;
    }

    @Test
    @DisplayName("立即购买生成 PENDING 交易单并锁定商品")
    void directCreatesPendingDeal() {
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(dealMapper.getActiveByItem(10L)).thenReturn(null);

        Map<String, Object> result = dealService.direct(2L, Map.of("itemId", 10L));

        assertThat(result).doesNotContainKey("errorCode");
        assertThat(result.get("status")).isEqualTo("PENDING");
        assertThat(result.get("source")).isEqualTo("DIRECT");
        ArgumentCaptor<Deal> captor = ArgumentCaptor.forClass(Deal.class);
        verify(dealMapper).insert(captor.capture());
        assertThat(captor.getValue().getDealPrice()).isEqualByComparingTo("25.00");
        assertThat(captor.getValue().getBuyerId()).isEqualTo(2L);
        verify(bargainMapper).rejectOthers(10L, -1L);
        verify(reservationMapper).closeActiveByItem(10L);
    }

    @Test
    @DisplayName("不能购买自己的商品（4003）")
    void directRejectsSelfPurchase() {
        when(itemMapper.getItemById(10L)).thenReturn(item);

        Map<String, Object> result = dealService.direct(1L, Map.of("itemId", 10L));

        assertThat(result.get("errorCode")).isEqualTo(4003);
        verify(dealMapper, never()).insert(any(Deal.class));
    }

    @Test
    @DisplayName("存在待成交交易单时不可重复下单（4005）")
    void directRejectsWhenDealActive() {
        when(itemMapper.getItemById(10L)).thenReturn(item);
        when(dealMapper.getActiveByItem(10L)).thenReturn(deal(9L, "PENDING", "DIRECT", null));

        Map<String, Object> result = dealService.direct(2L, Map.of("itemId", 10L));

        assertThat(result.get("errorCode")).isEqualTo(4005);
    }

    @Test
    @DisplayName("非交易方不能确认成交（4003）")
    void completeRejectsOutsider() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "PENDING", "DIRECT", null));

        assertThat(dealService.complete(9L, 3L)).isEqualTo(4003);
        verify(dealMapper, never()).complete(9L);
    }

    @Test
    @DisplayName("确认成交写入商品成交快照并关闭预约")
    void completeMarksItemSold() {
        Deal deal = deal(9L, "PENDING", "DIRECT", null);
        when(dealMapper.getById(9L)).thenReturn(deal);
        when(dealMapper.complete(9L)).thenReturn(1);

        int code = dealService.complete(9L, 1L);

        assertThat(code).isZero();
        verify(itemMapper).markSold(10L, 2L, new BigDecimal("25.00"));
        verify(reservationMapper).closeActiveByItem(10L);
    }

    @Test
    @DisplayName("已完成的交易单不能再次确认（4005）")
    void completeRejectsNonPending() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "COMPLETED", "DIRECT", null));

        assertThat(dealService.complete(9L, 1L)).isEqualTo(4005);
    }

    @Test
    @DisplayName("取消来源为预约的交易单时同步关闭预约")
    void cancelClosesReservation() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "PENDING", "RESERVATION", 77L));
        when(dealMapper.cancel(9L)).thenReturn(1);

        int code = dealService.cancel(9L, 2L);

        assertThat(code).isZero();
        verify(reservationMapper).updateStatus(77L, "CLOSED");
    }

    @Test
    @DisplayName("非交易方不能读取私聊（4003）")
    void messagesRejectOutsider() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "PENDING", "DIRECT", null));

        Map<String, Object> result = dealService.messages(9L, 3L);

        assertThat(result.get("errorCode")).isEqualTo(4003);
        verify(dealMessageMapper, never()).listByDeal(9L);
    }

    @Test
    @DisplayName("交易双方可读取私聊，读取后标记已读")
    void messagesVisibleToBuyer() {
        Deal deal = deal(9L, "PENDING", "DIRECT", null);
        when(dealMapper.getById(9L)).thenReturn(deal);
        DealMessage message = new DealMessage();
        message.setId(1L);
        message.setSenderId(1L);
        message.setContent("你好");
        message.setSenderNickname("卖家");
        when(dealMessageMapper.listByDeal(9L)).thenReturn(List.of(message));

        Map<String, Object> result = dealService.messages(9L, 2L);

        assertThat(result.get("peerNickname")).isEqualTo("卖家");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("mine")).isEqualTo(false);
        verify(dealMessageMapper).markReadByDeal(9L, 2L);
    }

    @Test
    @DisplayName("取消的交易单私聊只读（4005）")
    void sendMessageRejectsCancelledDeal() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "CANCELLED", "DIRECT", null));

        assertThat(dealService.sendMessage(9L, 2L, Map.of("content", "在吗"))).isEqualTo(4005);
    }

    @Test
    @DisplayName("私聊内容为空返回 4001")
    void sendMessageRejectsBlankContent() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "PENDING", "DIRECT", null));

        assertThat(dealService.sendMessage(9L, 2L, Map.of("content", "   "))).isEqualTo(4001);
    }

    @Test
    @DisplayName("买家发私聊时接收方自动为卖家")
    void sendMessageSetsReceiver() {
        when(dealMapper.getById(9L)).thenReturn(deal(9L, "PENDING", "DIRECT", null));

        assertThat(dealService.sendMessage(9L, 2L, Map.of("content", "明晚见"))).isZero();
        ArgumentCaptor<DealMessage> captor = ArgumentCaptor.forClass(DealMessage.class);
        verify(dealMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getReceiverId()).isEqualTo(1L);
        assertThat(captor.getValue().getContent()).isEqualTo("明晚见");
    }
}
