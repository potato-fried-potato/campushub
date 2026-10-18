package com.campushub.service;

import com.campushub.entity.Deal;
import com.campushub.entity.DealMessage;
import com.campushub.entity.Item;
import com.campushub.mapper.BargainMapper;
import com.campushub.mapper.DealMessageMapper;
import com.campushub.mapper.DealMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 交易单：立即购买 / 砍价接受 / 预约确认 三条成交路径的统一落点。
 *
 * PENDING 锁商品（item 仍为 ON_SALE，由 deal 的 active_item_id 唯一约束保证只有一张生效单）；
 * COMPLETED 时商品置 SOLD 并写成交快照；CANCELLED 时解锁。
 */
@Service
public class DealService {

    @Autowired
    private DealMapper dealMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BargainMapper bargainMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private DealMessageMapper dealMessageMapper;

    /**
     * 立即购买：按挂牌价生成待成交交易单（AC-10.1 / 10.3 / 10.4）
     */
    @Transactional
    public Map<String, Object> direct(Long userId, Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long itemId = toLong(body.get("itemId"));
        if (itemId == null) {
            return error(result, 4001, "请选择商品");
        }
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return error(result, 4004, "商品不存在");
        }
        if (item.getSellerId().equals(userId)) {
            return error(result, 4003, "不能购买自己发布的商品");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            return error(result, 4005, "商品当前不可购买");
        }
        if (dealMapper.getActiveByItem(itemId) != null) {
            return error(result, 4005, "商品已有待成交交易单");
        }
        Deal deal = newDeal(item, userId, item.getPrice(), "DIRECT", null);
        dealMapper.insert(deal);
        lockItem(itemId);
        result.put("id", deal.getId());
        result.put("status", "PENDING");
        result.put("source", "DIRECT");
        result.put("dealPrice", item.getPrice());
        return result;
    }

    /**
     * 砍价接受后生成交易单（由 BargainService 调用）
     */
    @Transactional
    public int createFromBargain(Item item, Long buyerId, BigDecimal price, Long bargainId) {
        if (dealMapper.getActiveByItem(item.getId()) != null) {
            return 4005;
        }
        Deal deal = newDeal(item, buyerId, price, "BARGAIN", bargainId);
        dealMapper.insert(deal);
        lockItem(item.getId());
        return 0;
    }

    /**
     * 预约确认后生成交易单（由 ReservationService 调用）
     */
    @Transactional
    public int createFromReservation(Item item, Long buyerId, BigDecimal price, Long reservationId) {
        if (dealMapper.getActiveByItem(item.getId()) != null) {
            return 4005;
        }
        Deal deal = newDeal(item, buyerId, price, "RESERVATION", reservationId);
        dealMapper.insert(deal);
        // 保留当前这条预约（已经是 CONFIRMED），关闭同商品其他预约与其他待处理出价
        bargainMapper.rejectOthers(item.getId(), -1L);
        reservationMapper.closeOthersByItem(item.getId(), reservationId);
        return 0;
    }

    public Map<String, Object> list(Long userId, String view, String status) {
        String queryView = "sent".equals(view) ? "sent" : "received";
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", dealMapper.countPending(userId, queryView));
        data.put("unreadMessageCount", dealMessageMapper.countUnread(userId));
        data.put("list", dealMapper.list(userId, queryView, status));
        return data;
    }

    /**
     * 交易单私密会话：只有该单的买家与卖家能看，与公开的商品问答相互独立（AC-10.8）
     */
    public Map<String, Object> messages(Long dealId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        Deal deal = dealMapper.getById(dealId);
        if (deal == null) {
            return error(result, 4004, "交易单不存在");
        }
        if (!isParticipant(deal, userId)) {
            return error(result, 4003, "只有交易双方可以查看该会话");
        }
        dealMessageMapper.markReadByDeal(dealId, userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (DealMessage m : dealMessageMapper.listByDeal(dealId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("mine", m.getSenderId().equals(userId));
            row.put("senderNickname", m.getSenderNickname());
            row.put("content", m.getContent());
            row.put("createdAt", m.getCreatedAt());
            list.add(row);
        }
        result.put("dealId", dealId);
        result.put("itemId", deal.getItemId());
        result.put("itemTitle", deal.getItemTitle());
        result.put("status", deal.getStatus());
        result.put("dealPrice", deal.getDealPrice());
        result.put("peerNickname", userId.equals(deal.getBuyerId())
                ? deal.getSellerNickname() : deal.getBuyerNickname());
        result.put("list", list);
        return result;
    }

    /**
     * 发送私聊：交易双方可发，已取消的交易单只读（AC-10.8）
     */
    public int sendMessage(Long dealId, Long userId, Map<String, Object> body) {
        Deal deal = dealMapper.getById(dealId);
        if (deal == null) {
            return 4004;
        }
        if (!isParticipant(deal, userId)) {
            return 4003;
        }
        if ("CANCELLED".equals(deal.getStatus())) {
            return 4005;
        }
        String content = body.get("content") == null ? null : String.valueOf(body.get("content")).trim();
        if (content == null || content.length() == 0 || content.length() > 200) {
            return 4001;
        }
        DealMessage message = new DealMessage();
        message.setDealId(dealId);
        message.setSenderId(userId);
        message.setReceiverId(userId.equals(deal.getBuyerId()) ? deal.getSellerId() : deal.getBuyerId());
        message.setContent(content);
        dealMessageMapper.insert(message);
        return 0;
    }

    private boolean isParticipant(Deal deal, Long userId) {
        return userId != null && (userId.equals(deal.getBuyerId()) || userId.equals(deal.getSellerId()));
    }

    /**
     * 确认成交：PENDING → COMPLETED，商品 SOLD + 成交快照（AC-10.6 / 10.7）
     */
    @Transactional
    public int complete(Long dealId, Long userId) {
        Deal deal = dealMapper.getById(dealId);
        if (deal == null) {
            return 4004;
        }
        if (!deal.getBuyerId().equals(userId) && !deal.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(deal.getStatus())) {
            return 4005;
        }
        if (dealMapper.complete(dealId) == 0) {
            return 4005;
        }
        itemMapper.markSold(deal.getItemId(), deal.getBuyerId(), deal.getDealPrice());
        reservationMapper.closeActiveByItem(deal.getItemId());
        return 0;
    }

    /**
     * 取消交易单：PENDING → CANCELLED，商品解锁（AC-10.5）
     */
    @Transactional
    public int cancel(Long dealId, Long userId) {
        Deal deal = dealMapper.getById(dealId);
        if (deal == null) {
            return 4004;
        }
        if (!deal.getBuyerId().equals(userId) && !deal.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(deal.getStatus())) {
            return 4005;
        }
        if (dealMapper.cancel(dealId) == 0) {
            return 4005;
        }
        // 来源为预约时，把该预约关闭，释放商品锁
        if ("RESERVATION".equals(deal.getSource()) && deal.getSourceId() != null) {
            reservationMapper.updateStatus(deal.getSourceId(), "CLOSED");
        }
        return 0;
    }

    public boolean hasActiveDeal(Long itemId) {
        return dealMapper.getActiveByItem(itemId) != null;
    }

    /**
     * 锁定商品：驳回其他待处理出价、关闭进行中预约。
     * 商品状态保持 ON_SALE，由 deal 的唯一约束表示锁定。
     */
    private void lockItem(Long itemId) {
        bargainMapper.rejectOthers(itemId, -1L);
        reservationMapper.closeActiveByItem(itemId);
    }

    private Deal newDeal(Item item, Long buyerId, BigDecimal price, String source, Long sourceId) {
        Deal deal = new Deal();
        deal.setItemId(item.getId());
        deal.setBuyerId(buyerId);
        deal.setSellerId(item.getSellerId());
        deal.setDealPrice(price);
        deal.setSource(source);
        deal.setSourceId(sourceId);
        return deal;
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
}
