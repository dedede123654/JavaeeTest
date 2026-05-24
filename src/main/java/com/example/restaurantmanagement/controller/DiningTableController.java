package com.example.restaurantmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.entity.DiningTable;
import com.example.restaurantmanagement.mapper.DiningTableMapper;
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
@RequestMapping("/api/tables")
public class DiningTableController {

    private final DiningTableMapper diningTableMapper;

    public DiningTableController(DiningTableMapper diningTableMapper) {
        this.diningTableMapper = diningTableMapper;
    }

    @GetMapping
    public ApiResponse<List<DiningTable>> list() {
        LambdaQueryWrapper<DiningTable> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(DiningTable::getId);
        return ApiResponse.success(diningTableMapper.selectList(queryWrapper));
    }

    @PostMapping
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<DiningTable> save(@RequestBody DiningTable diningTable) {
        if (diningTable.getStatus() == null || diningTable.getStatus().isBlank()) {
            diningTable.setStatus("IDLE");
        }
        diningTableMapper.insert(diningTable);
        return ApiResponse.success(diningTable);
    }

    @PutMapping("/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<DiningTable> update(@PathVariable Integer id, @RequestBody DiningTable diningTable) {
        diningTable.setId(id);
        diningTableMapper.updateById(diningTable);
        return ApiResponse.success(diningTable);
    }

    @DeleteMapping("/{id}")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        diningTableMapper.deleteById(id);
        return ApiResponse.success("删除成功");
    }
}
