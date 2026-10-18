package com.campushub.mapper;

import com.campushub.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReservationMapper {

    int insert(Reservation reservation);

    Reservation getById(@Param("id") Long id);

    List<Reservation> list(@Param("userId") Long userId,
                           @Param("view") String view,
                           @Param("status") String status);

    int countPending(@Param("userId") Long userId, @Param("view") String view);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int hasActive(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

    int hasConfirmed(@Param("itemId") Long itemId);

    int closeActiveByItem(@Param("itemId") Long itemId);

    // 关闭同商品其他进行中预约（保留指定的一条）
    int closeOthersByItem(@Param("itemId") Long itemId, @Param("exceptId") Long exceptId);

}
