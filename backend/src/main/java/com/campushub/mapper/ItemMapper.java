package com.campushub.mapper;

import com.campushub.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemMapper {

    // 首页在售商品列表
    List<Item> getItemList();

    // 某个用户发布的商品
    List<Item> getMyItems(@Param("userId") Long userId);

}
