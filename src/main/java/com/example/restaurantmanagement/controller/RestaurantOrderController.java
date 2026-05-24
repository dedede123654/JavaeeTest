package com.example.restaurantmanagement.controller;

import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.CreateOrderRequest;
import com.example.restaurantmanagement.dto.OrderPaymentRequest;
import com.example.restaurantmanagement.dto.OrderPriorityRequest;
import com.example.restaurantmanagement.dto.OrderView;
import com.example.restaurantmanagement.dto.WasteRecordRequest;
import com.example.restaurantmanagement.service.RestaurantOrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class RestaurantOrderController {

    private final RestaurantOrderService restaurantOrderService;

    public RestaurantOrderController(RestaurantOrderService restaurantOrderService) {
        this.restaurantOrderService = restaurantOrderService;
    }

    @GetMapping
    @RequireRoles({"OWNER", "MANAGER", "WAITER", "CASHIER"})
    public ApiResponse<List<OrderView>> list() {
        return ApiResponse.success(restaurantOrderService.listOrderViews());
    }

    @PostMapping
    public ApiResponse<OrderView> create(@RequestBody CreateOrderRequest request) {
        return ApiResponse.success(restaurantOrderService.createOrder(request));
    }

    @PostMapping("/{id}/pay")
    @RequireRoles({"OWNER", "MANAGER", "CASHIER"})
    public ApiResponse<OrderView> pay(@PathVariable Integer id, @RequestBody OrderPaymentRequest request) {
        return ApiResponse.success(restaurantOrderService.payOrder(id, request));
    }

    @PostMapping("/{id}/priority")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<OrderView> priority(@PathVariable Integer id, @RequestBody OrderPriorityRequest request) {
        return ApiResponse.success(restaurantOrderService.prioritizeOrder(id, request));
    }

    @PostMapping("/{id}/complete")
    @RequireRoles({"OWNER", "MANAGER", "CASHIER"})
    public ApiResponse<OrderView> complete(@PathVariable Integer id) {
        return ApiResponse.success(restaurantOrderService.completeOrder(id));
    }

    @PostMapping("/{id}/waste-records")
    @RequireRoles({"OWNER", "MANAGER", "WAITER"})
    public ApiResponse<OrderView> recordWaste(@PathVariable Integer id, @RequestBody List<WasteRecordRequest> requests) {
        return ApiResponse.success(restaurantOrderService.recordWaste(id, requests));
    }
}
