package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderPaymentRequest {

    private String paymentMethod;

    private BigDecimal discountAmount;
}
