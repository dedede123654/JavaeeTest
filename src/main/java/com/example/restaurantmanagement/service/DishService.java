package com.example.restaurantmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.restaurantmanagement.dto.DishMaterialRequest;
import com.example.restaurantmanagement.entity.Dish;
import com.example.restaurantmanagement.entity.DishMaterial;
import java.util.List;

public interface DishService extends IService<Dish> {

    List<DishMaterial> bindMaterials(Integer dishId, List<DishMaterialRequest> requests);

    List<DishMaterial> getDishMaterials(Integer dishId);
}
