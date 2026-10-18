package com.campushub.mapper;

import com.campushub.entity.Deal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DealMapper {

    int insert(Deal deal);

    Deal getById(@Param("id") Long id);

    // 当前商品的生效交易单（PENDING），没有则 null
    Deal getActiveByItem(@Param("itemId") Long itemId);

    // 最近一张已完成的交易单（评价与成交对手方判定）
    Deal getCompletedByItem(@Param("itemId") Long itemId);

    List<Deal> list(@Param("userId") Long userId,
                    @Param("view") String view,
                    @Param("status") String status);

    int countPending(@Param("userId") Long userId, @Param("view") String view);

    // 当前用户作为一方、已完成但尚未评价的交易单
    List<Deal> listPendingReview(@Param("userId") Long userId);

    int complete(@Param("id") Long id);

    int cancel(@Param("id") Long id);
}
