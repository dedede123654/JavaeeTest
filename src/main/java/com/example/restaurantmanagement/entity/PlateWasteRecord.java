package com.example.restaurantmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("plate_waste_record")
public class PlateWasteRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private Integer orderItemId;

    private Integer dishId;

    private String wasteLevel;

    private String note;

    private LocalDateTime createTime;
}
