package com.example.restaurantmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.DishMaterialRequest;
import com.example.restaurantmanagement.entity.Dish;
import com.example.restaurantmanagement.entity.DishCategory;
import com.example.restaurantmanagement.entity.DishMaterial;
import com.example.restaurantmanagement.mapper.DishCategoryMapper;
import com.example.restaurantmanagement.service.DishService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DishController {

    private final DishCategoryMapper dishCategoryMapper;
    private final DishService dishService;

    public DishController(DishCategoryMapper dishCategoryMapper, DishService dishService) {
        this.dishCategoryMapper = dishCategoryMapper;
        this.dishService = dishService;
    }

    @GetMapping("/dish-categories")
    public ApiResponse<List<DishCategory>> listCategories() {
        LambdaQueryWrapper<DishCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(DishCategory::getSortOrder).orderByAsc(DishCategory::getId);
        return ApiResponse.success(dishCategoryMapper.selectList(queryWrapper));
    }

    @PostMapping("/dish-categories")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<DishCategory> createCategory(@RequestBody DishCategory category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        dishCategoryMapper.insert(category);
        return ApiResponse.success(category);
    }

    @DeleteMapping("/dish-categories/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Void> deleteCategory(@PathVariable Integer id) {
        dishCategoryMapper.deleteById(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/dishes")
    public ApiResponse<List<Dish>> listDishes() {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Dish::getId);
        return ApiResponse.success(dishService.list(queryWrapper));
    }

    @PostMapping("/dishes")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Dish> createDish(@RequestBody Dish dish) {
        if (dish.getStatus() == null) {
            dish.setStatus(1);
        }
        dishService.save(dish);
        return ApiResponse.success(dish);
    }

    @PutMapping("/dishes/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Dish> updateDish(@PathVariable Integer id, @RequestBody Dish dish) {
        dish.setId(id);
        dishService.updateById(dish);
        return ApiResponse.success(dish);
    }

    @DeleteMapping("/dishes/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Void> deleteDish(@PathVariable Integer id) {
        dishService.removeById(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/dishes/{id}/materials")
    public ApiResponse<List<DishMaterial>> getMaterials(@PathVariable Integer id) {
        return ApiResponse.success(dishService.getDishMaterials(id));
    }

    @PostMapping("/dishes/{id}/materials")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<List<DishMaterial>> bindMaterials(@PathVariable Integer id,
                                                         @RequestBody List<DishMaterialRequest> requests) {
        return ApiResponse.success(dishService.bindMaterials(id, requests));
    }
}
