package com.example.restaurantmanagement.controller;

import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.DashboardOverviewResponse;
import com.example.restaurantmanagement.service.RestaurantOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequireRoles({"OWNER", "MANAGER", "WAITER", "CASHIER", "CHEF"})
public class DashboardController {

    private final RestaurantOrderService restaurantOrderService;

    public DashboardController(RestaurantOrderService restaurantOrderService) {
        this.restaurantOrderService = restaurantOrderService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.success(restaurantOrderService.getDashboardOverview());
    }
}
