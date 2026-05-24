package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardOverviewResponse {

    private Integer idleTables;

    private Integer diningTables;

    private Integer activeOrders;

    private Integer pendingKitchenItems;

    private Integer lowStockMaterials;

    private Integer priorityOrders;

    private BigDecimal todayRevenue;
}
