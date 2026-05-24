package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("material_stock")
public class MaterialStock {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer categoryId;

    private BigDecimal currentStock;

    private String unit;

    private BigDecimal warningStock;

    private Integer status;

    private LocalDateTime updateTime;
}
