package com.example.restaurantmanagement.controller;

import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.KitchenItemView;
import com.example.restaurantmanagement.dto.OrderView;
import com.example.restaurantmanagement.service.RestaurantOrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kitchen")
@RequireRoles({"OWNER", "MANAGER", "WAITER", "CHEF"})
public class KitchenController {

    private final RestaurantOrderService restaurantOrderService;

    public KitchenController(RestaurantOrderService restaurantOrderService) {
        this.restaurantOrderService = restaurantOrderService;
    }

    @GetMapping("/queue")
    public ApiResponse<List<KitchenItemView>> queue() {
        return ApiResponse.success(restaurantOrderService.listKitchenQueue());
    }

    @PostMapping("/items/{id}/start")
    @RequireRoles({"OWNER", "MANAGER", "CHEF"})
    public ApiResponse<OrderView> start(@PathVariable Integer id) {
        return ApiResponse.success(restaurantOrderService.startItem(id));
    }

    @PostMapping("/items/{id}/ready")
    @RequireRoles({"OWNER", "MANAGER", "CHEF"})
    public ApiResponse<OrderView> ready(@PathVariable Integer id) {
        return ApiResponse.success(restaurantOrderService.readyItem(id));
    }

    @PostMapping("/items/{id}/serve")
    @RequireRoles({"OWNER", "MANAGER", "WAITER", "CHEF"})
    public ApiResponse<OrderView> serve(@PathVariable Integer id) {
        return ApiResponse.success(restaurantOrderService.serveItem(id));
    }

    @PostMapping("/items/{id}/urge")
    @RequireRoles({"OWNER", "MANAGER", "WAITER"})
    public ApiResponse<OrderView> urge(@PathVariable Integer id) {
        return ApiResponse.success(restaurantOrderService.urgeItem(id));
    }
}
