package com.campushub.mapper;

import com.campushub.entity.DealMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DealMessageMapper {

    int insert(DealMessage message);

    List<DealMessage> listByDeal(@Param("dealId") Long dealId);

    int markReadByDeal(@Param("dealId") Long dealId, @Param("receiverId") Long receiverId);

    int countUnread(@Param("userId") Long userId);
}
