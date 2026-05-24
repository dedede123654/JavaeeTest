package com.example.restaurantmanagement.dto;

import lombok.Data;

@Data
public class OrderPriorityRequest {

    private Integer priorityLevel;

    private String priorityReason;
}
