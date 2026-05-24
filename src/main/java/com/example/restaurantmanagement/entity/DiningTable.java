package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dining_table")
public class DiningTable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String tableName;

    private Integer capacity;

    private String areaName;

    private String status;
}
