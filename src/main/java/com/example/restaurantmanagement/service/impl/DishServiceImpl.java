package com.example.restaurantmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.restaurantmanagement.dto.DishMaterialRequest;
import com.example.restaurantmanagement.entity.Dish;
import com.example.restaurantmanagement.entity.DishMaterial;
import com.example.restaurantmanagement.entity.MaterialStock;
import com.example.restaurantmanagement.exception.BusinessException;
import com.example.restaurantmanagement.mapper.DishMapper;
import com.example.restaurantmanagement.mapper.DishMaterialMapper;
import com.example.restaurantmanagement.mapper.MaterialStockMapper;
import com.example.restaurantmanagement.service.DishService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishMaterialMapper dishMaterialMapper;
    private final MaterialStockMapper materialStockMapper;

    public DishServiceImpl(DishMaterialMapper dishMaterialMapper, MaterialStockMapper materialStockMapper) {
        this.dishMaterialMapper = dishMaterialMapper;
        this.materialStockMapper = materialStockMapper;
    }

    @Override
    @Transactional
    public List<DishMaterial> bindMaterials(Integer dishId, List<DishMaterialRequest> requests) {
        Dish dish = getById(dishId);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }

        LambdaQueryWrapper<DishMaterial> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(DishMaterial::getDishId, dishId);
        dishMaterialMapper.delete(removeWrapper);

        List<DishMaterial> result = new ArrayList<>();
        if (requests == null) {
            return result;
        }

        for (DishMaterialRequest request : requests) {
            if (request.getMaterialId() == null || request.getRequiredQuantity() == null) {
                continue;
            }
            if (request.getRequiredQuantity().signum() <= 0) {
                throw new BusinessException("菜品原料用量必须大于 0");
            }
            MaterialStock material = materialStockMapper.selectById(request.getMaterialId());
            if (material == null) {
                throw new BusinessException("存在无效原料ID: " + request.getMaterialId());
            }
            DishMaterial dishMaterial = new DishMaterial();
            dishMaterial.setDishId(dishId);
            dishMaterial.setMaterialId(request.getMaterialId());
            dishMaterial.setRequiredQuantity(request.getRequiredQuantity());
            dishMaterialMapper.insert(dishMaterial);
            result.add(dishMaterial);
        }
        return result;
    }

    @Override
    public List<DishMaterial> getDishMaterials(Integer dishId) {
        LambdaQueryWrapper<DishMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishMaterial::getDishId, dishId);
        return dishMaterialMapper.selectList(queryWrapper);
    }
}
