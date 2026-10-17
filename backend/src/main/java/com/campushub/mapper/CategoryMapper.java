package com.campushub.mapper;

import com.campushub.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    // 分类列表，带各分类的在售数量
    List<Category> getCategoryList();

}
