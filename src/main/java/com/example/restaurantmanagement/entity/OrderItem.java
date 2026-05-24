package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private Integer dishId;

    private String dishNameSnapshot;

    private BigDecimal priceSnapshot;

    private Integer quantity;

    private String specialRequest;

    private String itemStatus;

    private Integer urgeCount;

    private LocalDateTime lastUrgedTime;

    private LocalDateTime createTime;
}
