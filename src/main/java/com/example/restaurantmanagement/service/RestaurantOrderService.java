package com.example.restaurantmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.restaurantmanagement.dto.CreateOrderRequest;
import com.example.restaurantmanagement.dto.AnalyticsSummaryResponse;
import com.example.restaurantmanagement.dto.DashboardOverviewResponse;
import com.example.restaurantmanagement.dto.KitchenItemView;
import com.example.restaurantmanagement.dto.OrderPaymentRequest;
import com.example.restaurantmanagement.dto.OrderPriorityRequest;
import com.example.restaurantmanagement.dto.OrderView;
import com.example.restaurantmanagement.dto.WasteRecordRequest;
import com.example.restaurantmanagement.entity.RestaurantOrder;
import java.util.List;

public interface RestaurantOrderService extends IService<RestaurantOrder> {

    OrderView createOrder(CreateOrderRequest request);

    List<OrderView> listOrderViews();

    OrderView payOrder(Integer orderId, OrderPaymentRequest request);

    OrderView prioritizeOrder(Integer orderId, OrderPriorityRequest request);

    OrderView completeOrder(Integer orderId);

    List<KitchenItemView> listKitchenQueue();

    OrderView startItem(Integer itemId);

    OrderView readyItem(Integer itemId);

    OrderView serveItem(Integer itemId);

    OrderView urgeItem(Integer itemId);

    DashboardOverviewResponse getDashboardOverview();

    OrderView recordWaste(Integer orderId, List<WasteRecordRequest> requests);

    AnalyticsSummaryResponse getAnalyticsSummary();
}
