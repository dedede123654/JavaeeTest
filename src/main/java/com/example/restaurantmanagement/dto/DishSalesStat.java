package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DishSalesStat {

    private String dishName;

    private Integer soldQuantity;

    private Integer wasteRecordedCount;

    private Integer wasteDishCount;

    private BigDecimal wasteRate;
}
