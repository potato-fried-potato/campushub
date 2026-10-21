package com.campushub.service;

import com.campushub.entity.ItemMessage;
import com.campushub.mapper.MessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M2/M3 站内留言：提问、回复与未读")
class MessageServiceTest {

    private static final Long ITEM_ID = 10L;
    private static final Long SELLER_ID = 1L;
    private static final Long BUYER_ID = 2L;

    @Mock private MessageMapper messageMapper;
    @InjectMocks private MessageService messageService;

    private ItemMessage message(Long id, Long senderId, Long parentId, Integer isRead) {
        ItemMessage m = new ItemMessage();
        m.setId(id);
        m.setItemId(ITEM_ID);
        m.setSenderId(senderId);
        m.setParentId(parentId);
        m.setIsRead(isRead);
        m.setContent("内容" + id);
        m.setCreatedAt(LocalDateTime.now());
        m.setItemTitle("九成新数据结构教材");
        m.setSenderNickname("发送者" + senderId);
        m.setReceiverNickname("接收者");
        return m;
    }

    @Test
    @DisplayName("空白内容返回 4001，且不写库")
    void blankContentRejected() {
        assertThat(messageService.post(BUYER_ID, ITEM_ID, "   ", null)).isEqualTo(4001);
        verify(messageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("超过 200 字返回 4001")
    void tooLongContentRejected() {
        assertThat(messageService.post(BUYER_ID, ITEM_ID, "a".repeat(201), null)).isEqualTo(4001);
    }

    @Test
    @DisplayName("商品不存在返回 4004")
    void unknownItemRejected() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(null);

        assertThat(messageService.post(BUYER_ID, ITEM_ID, "还在吗", null)).isEqualTo(4004);
    }

    @Test
    @DisplayName("卖家不能给自己的商品提问（4003）")
    void sellerCannotAskOwnItem() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);

        assertThat(messageService.post(SELLER_ID, ITEM_ID, "自问自答", null)).isEqualTo(4003);
    }

    @Test
    @DisplayName("买家提问成功：去除首尾空格，接收方自动为卖家")
    void buyerQuestionTargetsSeller() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);

        assertThat(messageService.post(BUYER_ID, ITEM_ID, " 可以面交吗 ", null)).isNull();

        ArgumentCaptor<ItemMessage> captor = ArgumentCaptor.forClass(ItemMessage.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("可以面交吗");
        assertThat(captor.getValue().getReceiverId()).isEqualTo(SELLER_ID);
        assertThat(captor.getValue().getParentId()).isNull();
    }

    @Test
    @DisplayName("回复不存在的父留言返回 4004")
    void replyToUnknownParentRejected() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);
        when(messageMapper.getById(5L)).thenReturn(null);

        assertThat(messageService.post(SELLER_ID, ITEM_ID, "回复", 5L)).isEqualTo(4004);
    }

    @Test
    @DisplayName("不能回复自己发出的留言（4003）")
    void cannotReplyToSelf() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);
        when(messageMapper.getById(5L)).thenReturn(message(5L, BUYER_ID, null, 0));

        assertThat(messageService.post(BUYER_ID, ITEM_ID, "再补一句", 5L)).isEqualTo(4003);
    }

    @Test
    @DisplayName("非话题参与者不能回复（4003）")
    void outsiderCannotReply() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);
        when(messageMapper.getById(5L)).thenReturn(message(5L, BUYER_ID, null, 0));

        assertThat(messageService.post(99L, ITEM_ID, "插一句", 5L)).isEqualTo(4003);
    }

    @Test
    @DisplayName("卖家回复买家提问：接收方为提问者")
    void sellerReplyTargetsBuyer() {
        when(messageMapper.getItemSellerId(ITEM_ID)).thenReturn(SELLER_ID);
        when(messageMapper.getById(5L)).thenReturn(message(5L, BUYER_ID, null, 0));

        assertThat(messageService.post(SELLER_ID, ITEM_ID, "可以，今晚七点", 5L)).isNull();

        ArgumentCaptor<ItemMessage> captor = ArgumentCaptor.forClass(ItemMessage.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(5L);
        assertThat(captor.getValue().getReceiverId()).isEqualTo(BUYER_ID);
    }

    @Test
    @DisplayName("收到的留言按商品聚合，并统计未读数")
    void listMineCountsUnread() {
        when(messageMapper.listMine(SELLER_ID, "received")).thenReturn(Arrays.asList(
                message(1L, BUYER_ID, null, 0),
                message(2L, BUYER_ID, null, 0),
                message(3L, BUYER_ID, null, 1)));

        Map<String, Object> data = messageService.listMine(SELLER_ID, "received");

        assertThat(data.get("totalUnread")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("unreadCount")).isEqualTo(2);
        assertThat(list.get(0).get("itemTitle")).isEqualTo("九成新数据结构教材");
    }

    @Test
    @DisplayName("view 传 sent 时查询发出的留言且不计未读")
    void listMineSentView() {
        when(messageMapper.listMine(BUYER_ID, "sent")).thenReturn(List.of(message(1L, BUYER_ID, null, 0)));

        Map<String, Object> data = messageService.listMine(BUYER_ID, "sent");

        assertThat(data.get("totalUnread")).isEqualTo(0);
        assertThat((List<?>) data.get("list")).hasSize(1);
    }

    @Test
    @DisplayName("标记已读：影响行数大于 0 时返回 true")
    void markReadReturnsTrueWhenUpdated() {
        when(messageMapper.markRead(5L, SELLER_ID)).thenReturn(1);

        assertThat(messageService.markRead(5L, SELLER_ID)).isTrue();
    }
}
