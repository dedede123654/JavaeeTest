package com.example.restaurantmanagement.controller;

import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.AnalyticsSummaryResponse;
import com.example.restaurantmanagement.service.RestaurantOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequireRoles({"OWNER", "MANAGER"})
public class AnalyticsController {

    private final RestaurantOrderService restaurantOrderService;

    public AnalyticsController(RestaurantOrderService restaurantOrderService) {
        this.restaurantOrderService = restaurantOrderService;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary() {
        return ApiResponse.success(restaurantOrderService.getAnalyticsSummary());
    }
}
