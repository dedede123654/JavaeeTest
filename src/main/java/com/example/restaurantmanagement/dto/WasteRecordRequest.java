package com.example.restaurantmanagement.dto;

import lombok.Data;

@Data
public class WasteRecordRequest {

    private Integer orderItemId;

    private String wasteLevel;

    private String note;
}
