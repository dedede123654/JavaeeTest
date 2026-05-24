package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer categoryId;

    private String name;

    private BigDecimal price;

    private Integer estimatedTime;

    private String imageUrl;

    private String description;

    private Integer status;

    private LocalDateTime createTime;
}
