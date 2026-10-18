package com.campushub.mapper;

import com.campushub.entity.Bargain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BargainMapper {

    int insert(Bargain bargain);

    Bargain getById(@Param("id") Long id);

    List<Bargain> list(@Param("userId") Long userId,
                       @Param("view") String view,
                       @Param("status") String status);

    int countPending(@Param("userId") Long userId, @Param("view") String view);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int rejectOthers(@Param("itemId") Long itemId, @Param("exceptId") Long exceptId);

    Long getAcceptedBuyerId(@Param("itemId") Long itemId);

    Long getPendingByItemAndBuyer(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

}
