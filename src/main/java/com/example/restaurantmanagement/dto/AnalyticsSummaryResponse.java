package com.example.restaurantmanagement.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class AnalyticsSummaryResponse {

    private BigDecimal totalRevenue;

    private Integer completedOrders;

    private Integer servedDishItems;

    private Integer wasteRecordedItems;

    private BigDecimal wasteRate;

    private List<DishSalesStat> topSellingDishes;

    private List<DishSalesStat> highWasteDishes;

    private List<MaterialAlertView> lowStockMaterials;
}
