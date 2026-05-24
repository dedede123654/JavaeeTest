package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateOrderItemRequest {

    private Integer dishId;

    private String customDishName;

    private BigDecimal customPrice;

    private Integer quantity;

    private String specialRequest;
}
