package com.campushub.service;

import com.campushub.entity.ItemMessage;
import com.campushub.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 发表留言 / 回复，返回 null 表示成功，否则返回业务码
     */
    public Integer post(Long userId, Long itemId, String content, Long parentId) {
        if (content == null || content.trim().length() == 0 || content.trim().length() > 200) {
            return 4001;
        }
        Long sellerId = messageMapper.getItemSellerId(itemId);
        if (sellerId == null) {
            return 4004;
        }
        ItemMessage message = new ItemMessage();
        message.setItemId(itemId);
        message.setSenderId(userId);
        message.setContent(content.trim());
        if (parentId == null) {
            if (userId.equals(sellerId)) {
                return 4003;
            }
            message.setReceiverId(sellerId);
        } else {
            ItemMessage parent = messageMapper.getById(parentId);
            if (parent == null || !parent.getItemId().equals(itemId)) {
                return 4004;
            }
            ItemMessage root = parent.getParentId() == null ? parent : messageMapper.getById(parent.getParentId());
            if (root == null) {
                return 4004;
            }
            boolean participant = userId.equals(root.getSenderId()) || userId.equals(sellerId);
            if (!participant || userId.equals(parent.getSenderId())) {
                return 4003;
            }
            message.setParentId(parentId);
            message.setReceiverId(parent.getSenderId());
        }
        messageMapper.insert(message);
        return null;
    }

    /**
     * 商品留言话题列表
     */
    public List<Map<String, Object>> listByItem(Long itemId) {
        List<ItemMessage> all = messageMapper.listByItem(itemId);
        List<Map<String, Object>> topics = new ArrayList<>();
        Map<Long, Map<String, Object>> topicMap = new LinkedHashMap<>();
        for (ItemMessage m : all) {
            if (m.getParentId() == null) {
                Map<String, Object> topic = new HashMap<>();
                topic.put("id", m.getId());
                topic.put("content", m.getContent());
                topic.put("sender", sender(m));
                topic.put("createdAt", m.getCreatedAt());
                topic.put("replies", new ArrayList<Map<String, Object>>());
                topicMap.put(m.getId(), topic);
            }
        }
        for (ItemMessage m : all) {
            if (m.getParentId() != null && topicMap.containsKey(m.getParentId())) {
                Map<String, Object> reply = new HashMap<>();
                reply.put("id", m.getId());
                reply.put("content", m.getContent());
                reply.put("sender", sender(m));
                reply.put("createdAt", m.getCreatedAt());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> replies = (List<Map<String, Object>>) topicMap.get(m.getParentId()).get("replies");
                replies.add(reply);
            }
        }
        // 话题按时间倒序（话题本身创建时间）
        List<Map<String, Object>> ordered = new ArrayList<>(topicMap.values());
        ordered.sort((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))));
        topics.addAll(ordered);
        return topics;
    }

    /**
     * 我的留言（收到的 / 发出的），按商品聚合
     */
    public Map<String, Object> listMine(Long userId, String view) {
        String queryView = "sent".equals(view) ? "sent" : "received";
        List<ItemMessage> rows = messageMapper.listMine(userId, queryView);
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        int totalUnread = 0;
        for (ItemMessage m : rows) {
            if ("received".equals(queryView) && m.getIsRead() != null && m.getIsRead() == 0) {
                totalUnread++;
            }
            Map<String, Object> summary = grouped.get(m.getItemId());
            if (summary == null) {
                summary = new HashMap<>();
                summary.put("itemId", m.getItemId());
                summary.put("itemTitle", m.getItemTitle());
                summary.put("peerNickname", "sent".equals(queryView) ? m.getReceiverNickname() : m.getSenderNickname());
                summary.put("lastContent", m.getContent());
                summary.put("unreadCount", 0);
                summary.put("lastTime", m.getCreatedAt());
                grouped.put(m.getItemId(), summary);
            }
            if ("received".equals(queryView) && m.getIsRead() != null && m.getIsRead() == 0) {
                summary.put("unreadCount", ((Integer) summary.get("unreadCount")) + 1);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("totalUnread", totalUnread);
        data.put("list", new ArrayList<>(grouped.values()));
        return data;
    }

    public boolean markRead(Long id, Long userId) {
        return messageMapper.markRead(id, userId) > 0;
    }

    private Map<String, Object> sender(ItemMessage m) {
        Map<String, Object> sender = new HashMap<>();
        sender.put("id", m.getSenderId());
        sender.put("nickname", m.getSenderNickname());
        return sender;
    }
}
