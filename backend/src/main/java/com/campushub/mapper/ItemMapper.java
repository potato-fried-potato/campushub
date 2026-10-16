package com.campushub.mapper;

import com.campushub.entity.Item;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ItemMapper {

    // 首页在售商品列表
    List<Item> getItemList();

}
