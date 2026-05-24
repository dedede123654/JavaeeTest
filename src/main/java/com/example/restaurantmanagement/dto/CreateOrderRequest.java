package com.example.restaurantmanagement.dto;

import java.util.List;
import lombok.Data;

@Data
public class CreateOrderRequest {

    private Integer tableId;

    private String source;

    private String remark;

    private String customerNote;

    private List<CreateOrderItemRequest> items;
}
