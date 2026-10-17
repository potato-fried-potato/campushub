package com.campushub.service;

import com.campushub.entity.Category;
import com.campushub.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> getCategoryList() {
        return categoryMapper.getCategoryList();
    }

}
