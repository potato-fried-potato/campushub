package com.campushub.entity;

/**
 * 商品分类
 */
public class Category {

    private Long id;
    private String name;
    private String icon;
    private Integer sortNo;

    // 联表统计出来的，不是数据库字段
    private Integer onSaleCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getOnSaleCount() {
        return onSaleCount;
    }

    public void setOnSaleCount(Integer onSaleCount) {
        this.onSaleCount = onSaleCount;
    }
}
