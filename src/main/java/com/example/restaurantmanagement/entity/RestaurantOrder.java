package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("restaurant_order")
public class RestaurantOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String orderNo;

    private Integer tableId;

    private String source;

    private String orderStatus;

    private String paymentMethod;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Integer priorityLevel;

    private String priorityReason;

    private String remark;

    private String customerNote;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;
}
