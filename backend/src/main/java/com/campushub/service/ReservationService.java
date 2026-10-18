package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Reservation;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private DealService dealService;

    public Map<String, Object> create(Long userId, Map<String, Object> body) {
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
            return error(result, 4003, "不能预约自己发布的商品");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            return error(result, 4005, "商品当前不可预约");
        }
        if (dealService.hasActiveDeal(itemId)) {
            return error(result, 4005, "商品已有待成交交易单，暂不可预约");
        }
        if (reservationMapper.hasActive(itemId, userId) > 0) {
            return error(result, 4005, "你已有一个进行中的预约");
        }
        Reservation reservation = new Reservation();
        reservation.setItemId(itemId);
        reservation.setBuyerId(userId);
        reservation.setExpectTime(parseTime(body.get("expectTime")));
        reservation.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        reservationMapper.insert(reservation);
        result.put("id", reservation.getId());
        result.put("status", "PENDING");
        return result;
    }

    public Map<String, Object> list(Long userId, String view, String status) {
        String queryView = "sent".equals(view) ? "sent" : "received";
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", reservationMapper.countPending(userId, queryView));
        data.put("list", reservationMapper.list(userId, queryView, status));
        return data;
    }

    @Transactional
    public int confirm(Long id, Long userId) {
        Reservation reservation = reservationMapper.getById(id);
        if (reservation == null) {
            return 4004;
        }
        if (!reservation.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(reservation.getStatus())) {
            return 4005;
        }
        Item item = itemMapper.getItemById(reservation.getItemId());
        if (item == null || !"ON_SALE".equals(item.getStatus())) {
            return 4005;
        }
        if (dealService.hasActiveDeal(reservation.getItemId())) {
            return 4005;
        }
        reservationMapper.updateStatus(id, "CONFIRMED");
        // 生成待成交交易单：驳回其他出价、关闭其他预约，商品锁定（AC-10.3）
        int code = dealService.createFromReservation(item, reservation.getBuyerId(),
                item.getPrice(), id);
        if (code != 0) {
            throw new IllegalStateException("生成交易单失败");
        }
        return 0;
    }

    public int reject(Long id, Long userId) {
        Reservation reservation = reservationMapper.getById(id);
        if (reservation == null) {
            return 4004;
        }
        if (!reservation.getSellerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(reservation.getStatus())) {
            return 4005;
        }
        reservationMapper.updateStatus(id, "REJECTED");
        return 0;
    }

    public int cancel(Long id, Long userId) {
        Reservation reservation = reservationMapper.getById(id);
        if (reservation == null) {
            return 4004;
        }
        if (!reservation.getBuyerId().equals(userId)) {
            return 4003;
        }
        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            return 4005;
        }
        reservationMapper.updateStatus(id, "CANCELLED");
        return 0;
    }

    private LocalDateTime parseTime(Object value) {
        try {
            return value == null ? null : LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
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
