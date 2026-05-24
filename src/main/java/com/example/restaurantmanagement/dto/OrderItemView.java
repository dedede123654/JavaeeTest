package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderItemView {

    private Integer id;

    private Integer dishId;

    private String dishName;

    private Integer quantity;

    private BigDecimal priceSnapshot;

    private String specialRequest;

    private String itemStatus;

    private Integer urgeCount;

    private LocalDateTime lastUrgedTime;

    private String wasteLevel;

    private String wasteNote;
}
