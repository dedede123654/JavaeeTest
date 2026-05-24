package com.example.restaurantmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.restaurantmanagement.entity.MaterialStock;
import com.example.restaurantmanagement.mapper.MaterialStockMapper;
import com.example.restaurantmanagement.service.MaterialStockService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MaterialStockServiceImpl extends ServiceImpl<MaterialStockMapper, MaterialStock>
        implements MaterialStockService {

    @Override
    public List<MaterialStock> refreshInventoryStatus() {
        List<MaterialStock> materials = list();
        for (MaterialStock material : materials) {
            material.setStatus(resolveStatus(material));
            material.setUpdateTime(LocalDateTime.now());
            updateById(material);
        }
        return list();
    }

    public static int resolveStatus(MaterialStock material) {
        BigDecimal current = material.getCurrentStock() == null ? BigDecimal.ZERO : material.getCurrentStock();
        BigDecimal warning = material.getWarningStock() == null ? BigDecimal.ZERO : material.getWarningStock();
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return 2;
        }
        if (current.compareTo(warning) <= 0) {
            return 1;
        }
        return 0;
    }
}
