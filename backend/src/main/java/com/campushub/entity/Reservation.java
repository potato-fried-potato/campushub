package com.campushub.entity;

import java.time.LocalDateTime;

/**
 * 意向预约
 */
public class Reservation {

    private Long id;
    private Long itemId;
    private Long buyerId;
    private LocalDateTime expectTime;
    private String remark;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    // 联表字段
    private String itemTitle;
    private String buyerNickname;
    private Long sellerId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public LocalDateTime getExpectTime() { return expectTime; }
    public void setExpectTime(LocalDateTime expectTime) { this.expectTime = expectTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
    public String getBuyerNickname() { return buyerNickname; }
    public void setBuyerNickname(String buyerNickname) { this.buyerNickname = buyerNickname; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}
