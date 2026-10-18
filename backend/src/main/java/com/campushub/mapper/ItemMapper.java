package com.campushub.mapper;

import com.campushub.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {

    // 集市商品列表（筛选 / 排序 / 分页）
    List<Item> getItemList(@Param("keyword") String keyword,
                           @Param("categoryId") Long categoryId,
                           @Param("conditionLevel") String conditionLevel,
                           @Param("campus") String campus,
                           @Param("status") String status,
                           @Param("sort") String sort,
                           @Param("excludeUserId") Long excludeUserId,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    // 集市商品总数
    int countItems(@Param("keyword") String keyword,
                   @Param("categoryId") Long categoryId,
                   @Param("conditionLevel") String conditionLevel,
                   @Param("campus") String campus,
                   @Param("status") String status,
                   @Param("sort") String sort,
                   @Param("excludeUserId") Long excludeUserId);

    // 某个用户发布的商品
    List<Item> getMyItems(@Param("userId") Long userId,
                          @Param("status") String status,
                          @Param("offset") int offset,
                          @Param("pageSize") int pageSize);

    // 某个用户发布的商品总数
    int countMyItems(@Param("userId") Long userId, @Param("status") String status);

    // 根据 id 查商品
    Item getItemById(@Param("id") Long id);

    // 商品图片列表（按 sort_no）
    List<String> getItemImages(@Param("itemId") Long itemId);

    // 详情页浏览量 +1
    int increaseViewCount(@Param("id") Long id);

    // 卖家信用评分统计
    Map<String, Object> getSellerRating(@Param("sellerId") Long sellerId);

    // 更新商品状态
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 发布商品
    int insertItem(Item item);

    // 保存商品图片
    int insertImage(@Param("itemId") Long itemId,
                    @Param("imageData") String imageData,
                    @Param("sortNo") int sortNo);

    // 管理后台：全部商品
    List<Item> adminList(@Param("status") String status,
                         @Param("keyword") String keyword,
                         @Param("offset") int offset,
                         @Param("pageSize") int pageSize);

    int adminCount(@Param("status") String status, @Param("keyword") String keyword);

}
