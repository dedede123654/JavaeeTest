package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DishMaterialRequest {

    private Integer materialId;

    private BigDecimal requiredQuantity;
}
