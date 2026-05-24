package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("dish_material")
public class DishMaterial {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer dishId;

    private Integer materialId;

    private BigDecimal requiredQuantity;
}
