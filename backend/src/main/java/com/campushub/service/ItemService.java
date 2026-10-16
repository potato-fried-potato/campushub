package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.mapper.ItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemMapper itemMapper;

    public List<Item> getItemList() {
        // TODO 后面要加分页、分类筛选、排序这些参数（AC-2.1 ~ 2.4）
        return itemMapper.getItemList();
    }

    public List<Item> getMyItems(Long userId) {
        return itemMapper.getMyItems(userId);
    }

}
