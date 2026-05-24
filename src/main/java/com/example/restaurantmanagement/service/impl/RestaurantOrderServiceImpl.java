package com.example.restaurantmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.restaurantmanagement.dto.AnalyticsSummaryResponse;
import com.example.restaurantmanagement.dto.CreateOrderItemRequest;
import com.example.restaurantmanagement.dto.CreateOrderRequest;
import com.example.restaurantmanagement.dto.DashboardOverviewResponse;
import com.example.restaurantmanagement.dto.DishSalesStat;
import com.example.restaurantmanagement.dto.KitchenItemView;
import com.example.restaurantmanagement.dto.MaterialAlertView;
import com.example.restaurantmanagement.dto.OrderItemView;
import com.example.restaurantmanagement.dto.OrderPaymentRequest;
import com.example.restaurantmanagement.dto.OrderPriorityRequest;
import com.example.restaurantmanagement.dto.OrderView;
import com.example.restaurantmanagement.dto.WasteRecordRequest;
import com.example.restaurantmanagement.entity.DiningTable;
import com.example.restaurantmanagement.entity.Dish;
import com.example.restaurantmanagement.entity.DishMaterial;
import com.example.restaurantmanagement.entity.MaterialStock;
import com.example.restaurantmanagement.entity.OrderItem;
import com.example.restaurantmanagement.entity.PlateWasteRecord;
import com.example.restaurantmanagement.entity.RestaurantOrder;
import com.example.restaurantmanagement.exception.BusinessException;
import com.example.restaurantmanagement.mapper.DiningTableMapper;
import com.example.restaurantmanagement.mapper.DishMapper;
import com.example.restaurantmanagement.mapper.DishMaterialMapper;
import com.example.restaurantmanagement.mapper.MaterialStockMapper;
import com.example.restaurantmanagement.mapper.OrderItemMapper;
import com.example.restaurantmanagement.mapper.PlateWasteRecordMapper;
import com.example.restaurantmanagement.mapper.RestaurantOrderMapper;
import com.example.restaurantmanagement.service.RestaurantOrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantOrderServiceImpl extends ServiceImpl<RestaurantOrderMapper, RestaurantOrder>
        implements RestaurantOrderService {

    private final DiningTableMapper diningTableMapper;
    private final DishMapper dishMapper;
    private final DishMaterialMapper dishMaterialMapper;
    private final MaterialStockMapper materialStockMapper;
    private final OrderItemMapper orderItemMapper;
    private final PlateWasteRecordMapper plateWasteRecordMapper;

    public RestaurantOrderServiceImpl(DiningTableMapper diningTableMapper,
                                      DishMapper dishMapper,
                                      DishMaterialMapper dishMaterialMapper,
                                      MaterialStockMapper materialStockMapper,
                                      OrderItemMapper orderItemMapper,
                                      PlateWasteRecordMapper plateWasteRecordMapper) {
        this.diningTableMapper = diningTableMapper;
        this.dishMapper = dishMapper;
        this.dishMaterialMapper = dishMaterialMapper;
        this.materialStockMapper = materialStockMapper;
        this.orderItemMapper = orderItemMapper;
        this.plateWasteRecordMapper = plateWasteRecordMapper;
    }

    @Override
    @Transactional
    public OrderView createOrder(CreateOrderRequest request) {
        if (request == null || request.getTableId() == null) {
            throw new BusinessException("请先选择桌台");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("订单至少需要一项菜品");
        }

        DiningTable diningTable = diningTableMapper.selectById(request.getTableId());
        if (diningTable == null) {
            throw new BusinessException("桌台不存在");
        }

        Map<Integer, Dish> dishMap = new HashMap<>();
        Map<Integer, BigDecimal> materialNeedMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new BusinessException("订单菜品数量不合法");
            }

            if (itemRequest.getDishId() == null) {
                BigDecimal customPrice = itemRequest.getCustomPrice();
                if (trimToNull(itemRequest.getCustomDishName()) == null) {
                    throw new BusinessException("定制菜需要填写菜名");
                }
                if (customPrice == null || customPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("定制菜价格必须大于 0");
                }
                totalAmount = totalAmount.add(customPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            } else {
                Dish dish = dishMapper.selectById(itemRequest.getDishId());
                if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
                    throw new BusinessException("存在不可下单菜品");
                }
                dishMap.put(dish.getId(), dish);
                totalAmount = totalAmount.add(dish.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

                LambdaQueryWrapper<DishMaterial> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DishMaterial::getDishId, dish.getId());
                List<DishMaterial> bindings = dishMaterialMapper.selectList(queryWrapper);
                if (bindings.isEmpty()) {
                    throw new BusinessException(dish.getName() + " 尚未配置原料");
                }
                for (DishMaterial binding : bindings) {
                    BigDecimal need = binding.getRequiredQuantity().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                    materialNeedMap.merge(binding.getMaterialId(), need, BigDecimal::add);
                }
            }
        }

        for (Map.Entry<Integer, BigDecimal> entry : materialNeedMap.entrySet()) {
            MaterialStock material = materialStockMapper.selectById(entry.getKey());
            BigDecimal current = material == null || material.getCurrentStock() == null
                    ? BigDecimal.ZERO
                    : material.getCurrentStock();
            if (material == null || current.compareTo(entry.getValue()) < 0) {
                throw new BusinessException((material == null ? "原料" : material.getName()) + " 库存不足");
            }
        }

        for (Map.Entry<Integer, BigDecimal> entry : materialNeedMap.entrySet()) {
            deductMaterialStock(entry.getKey(), entry.getValue());
        }

        RestaurantOrder order = new RestaurantOrder();
        order.setOrderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setTableId(request.getTableId());
        order.setSource(blankToDefault(request.getSource(), "WAITER"));
        order.setOrderStatus("PENDING_PAYMENT");
        order.setPaymentMethod(null);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(totalAmount);
        order.setPriorityLevel(0);
        order.setPriorityReason(null);
        order.setRemark(trimToNull(request.getRemark()));
        order.setCustomerNote(trimToNull(request.getCustomerNote()));
        order.setCreateTime(LocalDateTime.now());
        save(order);

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            if (itemRequest.getDishId() == null) {
                item.setDishId(null);
                item.setDishNameSnapshot(trimToNull(itemRequest.getCustomDishName()));
                item.setPriceSnapshot(itemRequest.getCustomPrice());
            } else {
                Dish dish = dishMap.get(itemRequest.getDishId());
                item.setDishId(dish.getId());
                item.setDishNameSnapshot(dish.getName());
                item.setPriceSnapshot(dish.getPrice());
            }
            item.setQuantity(itemRequest.getQuantity());
            item.setSpecialRequest(trimToNull(itemRequest.getSpecialRequest()));
            item.setItemStatus("PENDING");
            item.setUrgeCount(0);
            item.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(item);
        }

        diningTable.setStatus("DINING");
        diningTableMapper.updateById(diningTable);
        return buildOrderView(order);
    }

    @Override
    public List<OrderView> listOrderViews() {
        LambdaQueryWrapper<RestaurantOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(RestaurantOrder::getId);
        return list(queryWrapper).stream()
                .map(this::buildOrderView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderView payOrder(Integer orderId, OrderPaymentRequest request) {
        RestaurantOrder order = requireOrder(orderId);
        BigDecimal discount = request == null || request.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : request.getDiscountAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠金额不能为负数");
        }
        if (discount.compareTo(order.getTotalAmount()) > 0) {
            throw new BusinessException("优惠金额不能超过订单总额");
        }

        order.setDiscountAmount(discount);
        order.setFinalAmount(order.getTotalAmount().subtract(discount));
        order.setPaymentMethod(blankToDefault(request == null ? null : request.getPaymentMethod(), "移动支付"));
        order.setPaidTime(LocalDateTime.now());
        order.setOrderStatus(isAllItemsServed(orderId) ? "WAITING_CLEAR" : "PAID");
        updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public OrderView prioritizeOrder(Integer orderId, OrderPriorityRequest request) {
        RestaurantOrder order = requireOrder(orderId);
        int priorityLevel = request == null || request.getPriorityLevel() == null ? 1 : request.getPriorityLevel();
        if (priorityLevel < 1 || priorityLevel > 3) {
            throw new BusinessException("优先级只支持 1-3 级");
        }
        order.setPriorityLevel(priorityLevel);
        order.setPriorityReason(trimToNull(request.getPriorityReason()));
        updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public OrderView completeOrder(Integer orderId) {
        RestaurantOrder order = requireOrder(orderId);
        if (!isAllItemsServed(orderId)) {
            throw new BusinessException("仍有菜品未上桌，暂不能结单");
        }
        if (order.getPaidTime() == null) {
            throw new BusinessException("请先完成收款");
        }
        order.setOrderStatus("COMPLETED");
        updateById(order);
        releaseTable(order.getTableId());
        return buildOrderView(order);
    }

    @Override
    public List<KitchenItemView> listKitchenQueue() {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getItemStatus, "PENDING", "COOKING", "READY");
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        return items.stream()
                .map(this::buildKitchenItemView)
                .sorted(Comparator
                        .comparingInt((KitchenItemView item) -> normalizePriority(item.getPriorityLevel()))
                        .thenComparing(KitchenItemView::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderView startItem(Integer itemId) {
        return updateItemStatus(itemId, "PENDING", "COOKING");
    }

    @Override
    @Transactional
    public OrderView readyItem(Integer itemId) {
        return updateItemStatus(itemId, "COOKING", "READY");
    }

    @Override
    @Transactional
    public OrderView serveItem(Integer itemId) {
        OrderView view = updateItemStatus(itemId, "READY", "SERVED");
        RestaurantOrder order = requireOrder(view.getId());
        if (isAllItemsServed(order.getId()) && order.getPaidTime() != null) {
            order.setOrderStatus("WAITING_CLEAR");
            updateById(order);
            return buildOrderView(order);
        }
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public OrderView urgeItem(Integer itemId) {
        OrderItem item = requireItem(itemId);
        if ("SERVED".equals(item.getItemStatus())) {
            throw new BusinessException("已上桌菜品无需催单");
        }
        item.setUrgeCount((item.getUrgeCount() == null ? 0 : item.getUrgeCount()) + 1);
        item.setLastUrgedTime(LocalDateTime.now());
        orderItemMapper.updateById(item);
        return buildOrderView(requireOrder(item.getOrderId()));
    }

    @Override
    public DashboardOverviewResponse getDashboardOverview() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();

        response.setIdleTables(Math.toIntExact(diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStatus, "IDLE"))));
        response.setDiningTables(Math.toIntExact(diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getStatus, "DINING"))));
        response.setActiveOrders(Math.toIntExact(count(new LambdaQueryWrapper<RestaurantOrder>()
                .in(RestaurantOrder::getOrderStatus, "PENDING_PAYMENT", "PAID"))));
        response.setPendingKitchenItems(Math.toIntExact(orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getItemStatus, "PENDING", "COOKING", "READY"))));
        response.setLowStockMaterials(Math.toIntExact(materialStockMapper.selectCount(new LambdaQueryWrapper<MaterialStock>()
                .in(MaterialStock::getStatus, 1, 2))));
        response.setPriorityOrders(Math.toIntExact(count(new LambdaQueryWrapper<RestaurantOrder>()
                .gt(RestaurantOrder::getPriorityLevel, 0)
                .in(RestaurantOrder::getOrderStatus, "PENDING_PAYMENT", "PAID"))));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<RestaurantOrder> todayPaidOrders = list(new LambdaQueryWrapper<RestaurantOrder>()
                .ge(RestaurantOrder::getPaidTime, start)
                .lt(RestaurantOrder::getPaidTime, end));
        BigDecimal todayRevenue = todayPaidOrders.stream()
                .map(order -> order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTodayRevenue(todayRevenue);
        return response;
    }

    @Override
    @Transactional
    public OrderView recordWaste(Integer orderId, List<WasteRecordRequest> requests) {
        RestaurantOrder order = requireOrder(orderId);
        if (order.getPaidTime() == null) {
            throw new BusinessException("订单尚未完成收款，不能记录光盘情况");
        }
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("请至少提交一条光盘记录");
        }

        for (WasteRecordRequest request : requests) {
            if (request.getOrderItemId() == null) {
                throw new BusinessException("存在无效订单菜品");
            }
            OrderItem item = requireItem(request.getOrderItemId());
            if (!orderId.equals(item.getOrderId())) {
                throw new BusinessException("光盘记录与订单不匹配");
            }
            if (!"SERVED".equals(item.getItemStatus())) {
                throw new BusinessException("仅已上桌菜品可记录光盘情况");
            }
            String wasteLevel = normalizeWasteLevel(request.getWasteLevel());

            LambdaQueryWrapper<PlateWasteRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PlateWasteRecord::getOrderItemId, item.getId());
            List<PlateWasteRecord> existing = plateWasteRecordMapper.selectList(queryWrapper);

            PlateWasteRecord record = existing.isEmpty() ? new PlateWasteRecord() : existing.get(0);
            record.setOrderId(orderId);
            record.setOrderItemId(item.getId());
            record.setDishId(item.getDishId());
            record.setWasteLevel(wasteLevel);
            record.setNote(trimToNull(request.getNote()));
            if (record.getId() == null) {
                record.setCreateTime(LocalDateTime.now());
                plateWasteRecordMapper.insert(record);
            } else {
                plateWasteRecordMapper.updateById(record);
            }
        }
        if (isAllServedItemsWasteRecorded(orderId)) {
            order.setOrderStatus("COMPLETED");
            updateById(order);
            releaseTable(order.getTableId());
        }
        return buildOrderView(order);
    }

    @Override
    public AnalyticsSummaryResponse getAnalyticsSummary() {
        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse();

        List<RestaurantOrder> paidOrders = list(new LambdaQueryWrapper<RestaurantOrder>()
                .in(RestaurantOrder::getOrderStatus, "PAID", "COMPLETED"));
        List<RestaurantOrder> completedOrders = list(new LambdaQueryWrapper<RestaurantOrder>()
                .eq(RestaurantOrder::getOrderStatus, "COMPLETED"));
        List<OrderItem> servedItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getItemStatus, "SERVED"));
        List<PlateWasteRecord> wasteRecords = plateWasteRecordMapper.selectList(new LambdaQueryWrapper<>());
        List<MaterialStock> lowStockMaterials = materialStockMapper.selectList(new LambdaQueryWrapper<MaterialStock>()
                .in(MaterialStock::getStatus, 1, 2)
                .orderByAsc(MaterialStock::getStatus)
                .orderByAsc(MaterialStock::getCurrentStock));

        BigDecimal totalRevenue = paidOrders.stream()
                .map(order -> order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalRevenue(totalRevenue);
        response.setCompletedOrders(completedOrders.size());
        response.setServedDishItems(servedItems.size());
        response.setWasteRecordedItems(wasteRecords.size());
        response.setWasteRate(averageWasteRate(wasteRecords));

        Map<Integer, String> dishNameMap = dishMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(Dish::getId, Dish::getName, (left, right) -> left));

        Map<Integer, Integer> soldCountMap = new HashMap<>();
        for (OrderItem item : servedItems) {
            if (item.getDishId() != null) {
                soldCountMap.merge(item.getDishId(), item.getQuantity(), Integer::sum);
            }
        }

        Map<Integer, List<PlateWasteRecord>> wasteByDish = wasteRecords.stream()
                .filter(record -> record.getDishId() != null)
                .collect(Collectors.groupingBy(PlateWasteRecord::getDishId));

        List<DishSalesStat> dishStats = soldCountMap.entrySet().stream()
                .map(entry -> buildDishStat(entry.getKey(), entry.getValue(), wasteByDish.get(entry.getKey()), dishNameMap))
                .sorted(Comparator.comparingInt(DishSalesStat::getSoldQuantity).reversed())
                .collect(Collectors.toList());

        response.setTopSellingDishes(dishStats.stream().limit(5).collect(Collectors.toList()));
        response.setHighWasteDishes(dishStats.stream()
                .filter(stat -> stat.getWasteRecordedCount() != null && stat.getWasteRecordedCount() > 0)
                .sorted(Comparator.comparing(DishSalesStat::getWasteRate, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .limit(5)
                .collect(Collectors.toList()));

        response.setLowStockMaterials(lowStockMaterials.stream()
                .limit(5)
                .map(this::toMaterialAlertView)
                .collect(Collectors.toList()));
        return response;
    }

    private OrderView updateItemStatus(Integer itemId, String expectedStatus, String targetStatus) {
        OrderItem item = requireItem(itemId);
        if (!expectedStatus.equals(item.getItemStatus())) {
            throw new BusinessException("当前菜品状态不允许此操作");
        }
        item.setItemStatus(targetStatus);
        orderItemMapper.updateById(item);
        return buildOrderView(requireOrder(item.getOrderId()));
    }

    private RestaurantOrder requireOrder(Integer orderId) {
        RestaurantOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private OrderItem requireItem(Integer itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("订单菜品不存在");
        }
        return item;
    }

    private void releaseTable(Integer tableId) {
        DiningTable diningTable = diningTableMapper.selectById(tableId);
        if (diningTable != null) {
            diningTable.setStatus("IDLE");
            diningTableMapper.updateById(diningTable);
        }
    }

    private void deductMaterialStock(Integer materialId, BigDecimal requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        MaterialStock currentMaterial = materialStockMapper.selectById(materialId);
        String materialName = currentMaterial == null ? "原料" : currentMaterial.getName();
        int updated = materialStockMapper.update(
                null,
                new LambdaUpdateWrapper<MaterialStock>()
                        .eq(MaterialStock::getId, materialId)
                        .ge(MaterialStock::getCurrentStock, requiredQuantity)
                        .setSql("current_stock = current_stock - " + requiredQuantity.toPlainString())
                        .set(MaterialStock::getUpdateTime, LocalDateTime.now())
        );
        if (updated <= 0) {
            throw new BusinessException(materialName + " 库存不足，请刷新后重试");
        }

        MaterialStock latestMaterial = materialStockMapper.selectById(materialId);
        if (latestMaterial != null) {
            latestMaterial.setStatus(MaterialStockServiceImpl.resolveStatus(latestMaterial));
            latestMaterial.setUpdateTime(LocalDateTime.now());
            materialStockMapper.updateById(latestMaterial);
        }
    }

    private boolean isAllItemsServed(Integer orderId) {
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(queryWrapper);
        return !items.isEmpty() && items.stream().allMatch(item -> "SERVED".equals(item.getItemStatus()));
    }

    private boolean isAllServedItemsWasteRecorded(Integer orderId) {
        List<OrderItem> servedItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getItemStatus, "SERVED"));
        if (servedItems.isEmpty()) {
            return false;
        }

        long recordedCount = plateWasteRecordMapper.selectCount(new LambdaQueryWrapper<PlateWasteRecord>()
                .eq(PlateWasteRecord::getOrderId, orderId));
        return recordedCount >= servedItems.size();
    }

    private OrderView buildOrderView(RestaurantOrder order) {
        DiningTable diningTable = diningTableMapper.selectById(order.getTableId());
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId()).orderByAsc(OrderItem::getId);
        Map<Integer, PlateWasteRecord> wasteMap = plateWasteRecordMapper.selectList(new LambdaQueryWrapper<PlateWasteRecord>()
                        .eq(PlateWasteRecord::getOrderId, order.getId()))
                .stream()
                .collect(Collectors.toMap(PlateWasteRecord::getOrderItemId, item -> item, (left, right) -> left));

        List<OrderItemView> itemViews = orderItemMapper.selectList(itemWrapper).stream()
                .map(item -> toOrderItemView(item, wasteMap.get(item.getId())))
                .collect(Collectors.toList());

        OrderView view = new OrderView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setTableId(order.getTableId());
        view.setTableName(diningTable == null ? "未知桌台" : diningTable.getTableName());
        view.setSource(order.getSource());
        view.setOrderStatus(order.getOrderStatus());
        view.setPaymentMethod(order.getPaymentMethod());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setFinalAmount(order.getFinalAmount());
        view.setPriorityLevel(order.getPriorityLevel());
        view.setPriorityReason(order.getPriorityReason());
        view.setRemark(order.getRemark());
        view.setCustomerNote(order.getCustomerNote());
        view.setPaidTime(order.getPaidTime());
        view.setCreateTime(order.getCreateTime());
        view.setItems(itemViews);
        return view;
    }

    private OrderItemView toOrderItemView(OrderItem item, PlateWasteRecord wasteRecord) {
        OrderItemView view = new OrderItemView();
        view.setId(item.getId());
        view.setDishId(item.getDishId());
        view.setDishName(item.getDishNameSnapshot());
        view.setQuantity(item.getQuantity());
        view.setPriceSnapshot(item.getPriceSnapshot());
        view.setSpecialRequest(item.getSpecialRequest());
        view.setItemStatus(item.getItemStatus());
        view.setUrgeCount(item.getUrgeCount());
        view.setLastUrgedTime(item.getLastUrgedTime());
        if (wasteRecord != null) {
            view.setWasteLevel(wasteRecord.getWasteLevel());
            view.setWasteNote(wasteRecord.getNote());
        }
        return view;
    }

    private KitchenItemView buildKitchenItemView(OrderItem item) {
        RestaurantOrder order = requireOrder(item.getOrderId());
        DiningTable diningTable = diningTableMapper.selectById(order.getTableId());
        KitchenItemView view = new KitchenItemView();
        view.setItemId(item.getId());
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setTableId(order.getTableId());
        view.setTableName(diningTable == null ? "未知桌台" : diningTable.getTableName());
        view.setDishName(item.getDishNameSnapshot());
        view.setQuantity(item.getQuantity());
        view.setSpecialRequest(item.getSpecialRequest());
        view.setItemStatus(item.getItemStatus());
        view.setPriorityLevel(order.getPriorityLevel());
        view.setPriorityReason(order.getPriorityReason());
        view.setUrgeCount(item.getUrgeCount());
        view.setCreateTime(item.getCreateTime());
        view.setWaitMinutes(Duration.between(item.getCreateTime(), LocalDateTime.now()).toMinutes());
        return view;
    }

    private int normalizePriority(Integer priorityLevel) {
        if (priorityLevel == null || priorityLevel <= 0) {
            return 9;
        }
        return priorityLevel;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private String normalizeWasteLevel(String wasteLevel) {
        String normalized = wasteLevel == null ? "" : wasteLevel.trim();
        if (!"CLEAN".equals(normalized) && !"LEFTOVER_SOME".equals(normalized) && !"LEFTOVER_MUCH".equals(normalized)) {
            throw new BusinessException("光盘记录状态无效");
        }
        return normalized;
    }

    private boolean isWasteRecord(PlateWasteRecord record) {
        return !"CLEAN".equals(record.getWasteLevel());
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private DishSalesStat buildDishStat(Integer dishId,
                                        Integer soldQuantity,
                                        List<PlateWasteRecord> wasteRecords,
                                        Map<Integer, String> dishNameMap) {
        List<PlateWasteRecord> records = wasteRecords == null ? List.of() : wasteRecords;
        long wasteCount = records.stream().filter(this::isWasteRecord).count();

        DishSalesStat stat = new DishSalesStat();
        stat.setDishName(dishNameMap.getOrDefault(dishId, "菜品 #" + dishId));
        stat.setSoldQuantity(soldQuantity);
        stat.setWasteRecordedCount(records.size());
        stat.setWasteDishCount(Math.toIntExact(wasteCount));
        stat.setWasteRate(averageWasteRate(records));
        return stat;
    }

    private MaterialAlertView toMaterialAlertView(MaterialStock material) {
        MaterialAlertView view = new MaterialAlertView();
        view.setMaterialName(material.getName());
        view.setCurrentStock(material.getCurrentStock());
        view.setWarningStock(material.getWarningStock());
        view.setUnit(material.getUnit());
        view.setStatus(material.getStatus());
        return view;
    }

    private BigDecimal averageWasteRate(List<PlateWasteRecord> records) {
        if (records == null || records.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = records.stream()
                .map(this::wastePercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal wastePercent(PlateWasteRecord record) {
        if (record == null || record.getWasteLevel() == null) {
            return BigDecimal.ZERO;
        }
        return switch (record.getWasteLevel()) {
            case "LEFTOVER_SOME" -> BigDecimal.valueOf(20);
            case "LEFTOVER_MUCH" -> BigDecimal.valueOf(60);
            default -> BigDecimal.ZERO;
        };
    }
}
