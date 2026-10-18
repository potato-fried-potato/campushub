package com.campushub.mapper;

import com.campushub.entity.ItemMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insert(ItemMessage message);

    ItemMessage getById(@Param("id") Long id);

    Long getItemSellerId(@Param("itemId") Long itemId);

    List<ItemMessage> listByItem(@Param("itemId") Long itemId);

    List<ItemMessage> listMine(@Param("userId") Long userId, @Param("view") String view);

    int markRead(@Param("id") Long id, @Param("receiverId") Long receiverId);

}
