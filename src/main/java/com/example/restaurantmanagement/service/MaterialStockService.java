package com.example.restaurantmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.restaurantmanagement.entity.MaterialStock;
import java.util.List;

public interface MaterialStockService extends IService<MaterialStock> {

    List<MaterialStock> refreshInventoryStatus();
}
