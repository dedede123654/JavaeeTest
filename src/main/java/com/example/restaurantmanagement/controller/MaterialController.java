package com.example.restaurantmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.entity.MaterialCategory;
import com.example.restaurantmanagement.entity.MaterialStock;
import com.example.restaurantmanagement.mapper.MaterialCategoryMapper;
import com.example.restaurantmanagement.service.MaterialStockService;
import com.example.restaurantmanagement.service.impl.MaterialStockServiceImpl;
import java.time.LocalDateTime;
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
@RequireRoles({"OWNER", "MANAGER", "WAITER", "CASHIER", "CHEF"})
public class MaterialController {

    private final MaterialCategoryMapper materialCategoryMapper;
    private final MaterialStockService materialStockService;

    public MaterialController(MaterialCategoryMapper materialCategoryMapper, MaterialStockService materialStockService) {
        this.materialCategoryMapper = materialCategoryMapper;
        this.materialStockService = materialStockService;
    }

    @GetMapping("/material-categories")
    public ApiResponse<List<MaterialCategory>> listCategories() {
        LambdaQueryWrapper<MaterialCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(MaterialCategory::getId);
        return ApiResponse.success(materialCategoryMapper.selectList(queryWrapper));
    }

    @PostMapping("/material-categories")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<MaterialCategory> createCategory(@RequestBody MaterialCategory category) {
        materialCategoryMapper.insert(category);
        return ApiResponse.success(category);
    }

    @DeleteMapping("/material-categories/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Void> deleteCategory(@PathVariable Integer id) {
        materialCategoryMapper.deleteById(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/materials")
    public ApiResponse<List<MaterialStock>> listMaterials() {
        LambdaQueryWrapper<MaterialStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(MaterialStock::getId);
        return ApiResponse.success(materialStockService.list(queryWrapper));
    }

    @PostMapping("/materials")
    @RequireRoles({"OWNER", "MANAGER", "CHEF"})
    public ApiResponse<MaterialStock> createMaterial(@RequestBody MaterialStock material) {
        material.setStatus(MaterialStockServiceImpl.resolveStatus(material));
        material.setUpdateTime(LocalDateTime.now());
        materialStockService.save(material);
        return ApiResponse.success(material);
    }

    @PutMapping("/materials/{id}")
    @RequireRoles({"OWNER", "MANAGER", "CHEF"})
    public ApiResponse<MaterialStock> updateMaterial(@PathVariable Integer id, @RequestBody MaterialStock material) {
        material.setId(id);
        material.setStatus(MaterialStockServiceImpl.resolveStatus(material));
        material.setUpdateTime(LocalDateTime.now());
        materialStockService.updateById(material);
        return ApiResponse.success(material);
    }

    @DeleteMapping("/materials/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Void> deleteMaterial(@PathVariable Integer id) {
        materialStockService.removeById(id);
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/materials/refresh-status")
    @RequireRoles({"OWNER", "MANAGER", "CHEF"})
    public ApiResponse<List<MaterialStock>> refreshStatus() {
        return ApiResponse.success(materialStockService.refreshInventoryStatus());
    }
}
