package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dish_category")
public class DishCategory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String categoryName;

    private Integer sortOrder;
}
