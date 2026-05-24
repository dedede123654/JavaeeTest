package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class OrderView {

    private Integer id;

    private String orderNo;

    private Integer tableId;

    private String tableName;

    private String source;

    private String orderStatus;

    private String paymentMethod;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Integer priorityLevel;

    private String priorityReason;

    private String remark;

    private String customerNote;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;

    private List<OrderItemView> items;
}
