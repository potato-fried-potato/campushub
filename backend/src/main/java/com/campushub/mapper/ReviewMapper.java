package com.campushub.mapper;

import com.campushub.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper {

    int insert(Review review);

    int exists(@Param("itemId") Long itemId, @Param("reviewerId") Long reviewerId);

    List<Review> listByTarget(@Param("userId") Long userId);

    Map<String, Object> summary(@Param("userId") Long userId);

    // 我发出的评价
    List<Review> listByReviewer(@Param("userId") Long userId);

}
