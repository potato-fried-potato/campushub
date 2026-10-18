package com.campushub.service;

import com.campushub.entity.Bargain;
import com.campushub.entity.Item;
import com.campushub.mapper.BargainMapper;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class BargainService {

    @Autowired
    private BargainMapper bargainMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    public Map<String, Object> create(Long userId, Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long itemId = toLong(body.get("itemId"));
        BigDecimal offerPrice = toDecimal(body.get("offerPrice"));
        if (itemId == null || offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return error(result, 4001, "出价参数不合法");
        }
        Item item = itemMapper.getItemById(itemId);
        if (item == null) {
            return error(result, 4004, "商品不存在");
        }
        if (item.getSellerId().equals(userId)) {
            return error(result, 4003, "不能对自己的商品出价");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            return error(result, 4005, "商品当前不可出价");
        }
        if (offerPrice.compareTo(item.getPrice()) >= 0) {
            return error(result, 4001, "出价必须低于挂牌价");
        }
        if (bargainMapper.getPendingByItemAndBuyer(itemId, userId) != null) {
            return error(result, 4005, "你已有一个待处理出价");
        }
        Bargain bargain = new Bargain();
        bargain.setItemId(itemId);
        bargain.setBuyerId(userId);
        bargain.setOfferPrice(offerPrice);
        bargain.setMessage(body.get("message") == null ? null : String.valueOf(body.get("message")));
        bargainMapper.insert(bargain);
        result.put("id", bargain.getId());
        result.put("status", "PENDING");
        return result;
    }

    public Map<String, Object> list(Long userId, String view, String status) {
        String queryView = "sent".equals(view) ? "sent" : "received";
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", bargainMapper.countPending(userId, queryView));
        data.put("list", bargainMapper.list(userId, queryView, status));
        return data;
    }

    @Transactional
    public int accept(Long id, Long userId) {
        Bargain bargain = bargainMapper.getById(id);
        if (bargain == null) {
            return 4004;
        }
        if (!bargain.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            return 4005;
        }
        Item item = itemMapper.getItemById(bargain.getItemId());
        if (item == null || !"ON_SALE".equals(item.getStatus())) {
            return 4005;
        }
        bargainMapper.updateStatus(id, "ACCEPTED");
        bargainMapper.rejectOthers(bargain.getItemId(), id);
        itemMapper.updateStatus(bargain.getItemId(), "SOLD");
        reservationMapper.closeActiveByItem(bargain.getItemId());
        return 0;
    }

    public int reject(Long id, Long userId) {
        Bargain bargain = bargainMapper.getById(id);
        if (bargain == null) {
            return 4004;
        }
        if (!bargain.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            return 4005;
        }
        bargainMapper.updateStatus(id, "REJECTED");
        return 0;
    }

    public int cancel(Long id, Long userId) {
        Bargain bargain = bargainMapper.getById(id);
        if (bargain == null) {
            return 4004;
        }
        if (!bargain.getBuyerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            return 4005;
        }
        bargainMapper.updateStatus(id, "CANCELLED");
        return 0;
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

    private BigDecimal toDecimal(Object value) {
        try {
            return value == null ? null : new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
