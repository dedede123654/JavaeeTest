package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MaterialAlertView {

    private String materialName;

    private BigDecimal currentStock;

    private BigDecimal warningStock;

    private String unit;

    private Integer status;
}
