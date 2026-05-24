package com.example.restaurantmanagement.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KitchenItemView {

    private Integer itemId;

    private Integer orderId;

    private String orderNo;

    private Integer tableId;

    private String tableName;

    private String dishName;

    private Integer quantity;

    private String specialRequest;

    private String itemStatus;

    private Integer priorityLevel;

    private String priorityReason;

    private Integer urgeCount;

    private Long waitMinutes;

    private LocalDateTime createTime;
}
